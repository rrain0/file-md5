package md5.stage3read;

import md5.event.Event;
import md5.event.EventManager;
import md5.event.SubscriptionHolder;
import md5.stage1sourcesdata.Source;
import md5.stage1sourcesdata.SourceEv;
import md5.stage1sourcesdata.SourceEvType;
import md5.stage2estimate.EstimateEv;
import md5.stage2estimate.EstimateEvType;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class ReadManager implements Runnable{

    private static class SourceInfoBox {
        int waitForStart;
        int curr = 0;
        final List<SourceFiles> srcFiles;
        public SourceInfoBox(List<SourceFiles> srcFiles) {
            this.srcFiles = srcFiles;
            waitForStart = srcFiles.size();
        }
    }

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    private Map<Source,SourceFiles> srcToSrcFilesMap;

    // Map<readThreadId, SourceInfoBox>
    private Map<Object, SourceInfoBox> threadMap;
    private Set<SourceFiles> paused;

    public ReadManager(EventManager eventManager) {
        this.eventManager = eventManager;
        subscribe();
    }

    private void subscribe(){
        holder = eventManager.subscribe(incomeEvents::put);
    }
    private void unsubscribe(){
        holder.unsubscribe();
        incomeEvents.clear();
    }


    public void run(){
        try {
            start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void start() throws InterruptedException {

        loop: while (true){
            var event = incomeEvents.take();
            switch (event){
                case SourceEv ev && ev.type== SourceEvType.ALL_READY -> {
                    srcToSrcFilesMap = ev.sources.stream().collect(HashMap::new, (map,s)->map.put(s,new SourceFiles(s)), Map::putAll);
                    List<SourceFiles> sourceFilesList = ev.sources.stream().map(srcToSrcFilesMap::get).toList();
                    threadMap = ev.sources.stream().collect(Collectors.groupingBy(
                        Source::readThreadId, Collectors.collectingAndThen(
                            Collectors.mapping(srcToSrcFilesMap::get, Collectors.toList()), SourceInfoBox::new
                        ))
                    );
                    paused = Collections.synchronizedSet(new HashSet<>(sourceFilesList));
                    new Thread(this::work).start();
                }
                case EstimateEv ev && ev.type==EstimateEvType.FILE_FOUND -> {
                    srcToSrcFilesMap.get(ev.fileInfo.src()).files.add(ev.fileInfo);
                }
                case EstimateEv ev && ev.type==EstimateEvType.SOURCE_VIEWED -> {
                    var box = threadMap.get(ev.fileInfo.src().readThreadId());
                    box.waitForStart--;
                    if (box.waitForStart==0) {
                        for (int i = 0; i < box.srcFiles.size(); i++) {
                            var srcFiles = box.srcFiles.get(i);
                            new Thread(new ReadTask(srcFiles, this, eventManager)).start();
                            if (i==0) paused.remove(srcFiles);
                        }
                    }
                }
                case EstimateEv ev && ev.type==EstimateEvType.ALL_READY -> {
                    unsubscribe();
                    break loop;
                }
                /*case EstimateEv ev && ev.type==EstimateEvType.ALL_READY -> {

                    threadMap.forEach((id, box)->{
                        for (int i = 0; i < box.srcFiles.size(); i++) {
                            var source = box.srcFiles.get(i);
                            new Thread(new ReadTask(source, this, eventManager)).start();
                            if (i==0) paused.remove(source);
                        }

                    });

                    new Thread(this::work).start();

                }*/
                default -> {}
            }
        }

    }

    synchronized private void work() {
        try {
            while (!threadMap.isEmpty()){
                threadMap.forEach((id, box)->{
                    if (paused.containsAll(box.srcFiles)) {
                        box.curr = (box.curr+1) % box.srcFiles.size();
                        paused.remove(box.srcFiles.get(box.curr));
                    }
                });
                this.notifyAll();
                this.wait();
            }
            eventManager.addEvent(new ReadEv(ReadEvType.ALL_READY, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void awaitForWork(SourceFiles source) throws InterruptedException {
        while (paused.contains(source)) this.wait();
    }

    synchronized public void oneFileWasRead(SourceFiles source){
        paused.add(source);
        this.notifyAll();
    }

    synchronized public void workFinished(SourceFiles source){
        paused.remove(source);
        threadMap.compute(source.src.readThreadId(),(tId, box)->{
            box.srcFiles.remove(source);
            return box.srcFiles.size()==0 ? null : box;
        });
        this.notifyAll();
    }


}
