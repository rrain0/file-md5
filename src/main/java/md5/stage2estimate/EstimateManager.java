package md5.stage2estimate;

import md5.event.Event;
import md5.event.EventManager;
import md5.event.SubscriptionHolder;
import md5.stage1sourcesdata.Source;
import md5.stage1sourcesdata.SourceEv;
import md5.stage1sourcesdata.SourceEvType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

// todo менеджер будет считать кто сколько файлов прочитал и переключать потоки
// todo менеджер будет пихать файлы в кэш

public class EstimateManager implements Runnable{

    private static class SourceInfoBox {
        int curr = 0;
        final List<Source> srcs;
        public SourceInfoBox(List<Source> srcs) {
            this.srcs = srcs;
        }
    }

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    // Map<readThreadId, SourceInfoBox>
    private Map<Object, SourceInfoBox> sourceMap;
    private Set<Source> paused;

    public EstimateManager(EventManager eventManager) {
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


    @Override
    public void run(){
        try {
            start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void start() throws InterruptedException {

        while (true){
            Event<?> event = incomeEvents.take();
            if (event instanceof SourceEv ev && ev.type==SourceEvType.ALL_READY){
                unsubscribe();

                var sources = ev.sources;

                sourceMap = sources.stream().collect(Collectors.groupingBy(
                    Source::readThreadId,
                    Collectors.collectingAndThen(Collectors.toList(), SourceInfoBox::new)
                ));
                paused = new HashSet<>(sources);

                sourceMap.forEach((id,box)->{
                    for (int i = 0; i < box.srcs.size(); i++) {
                        var source = box.srcs.get(i);
                        new Thread(new EstimateTask(box.srcs.get(i), this, eventManager)).start();
                        if (i==0) paused.remove(source);
                    }
                });

                new Thread(this::work).start();

                break;
            }
        }
    }

    synchronized private void work() {
        try {
            while (!sourceMap.isEmpty()){
                sourceMap.forEach((id,box)->{
                    if (paused.containsAll(box.srcs)) {
                        box.curr = (box.curr+1) % box.srcs.size();
                        paused.remove(box.srcs.get(box.curr));
                    }
                });
                this.notifyAll();
                this.wait();
            }
            eventManager.addEvent(new EstimateEv(EstimateEvType.ALL_READY, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void awaitForWork(Source source) throws InterruptedException {
        while (paused.contains(source)) this.wait();
    }


    synchronized public void fileFound(Source source){
        paused.add(source);
        this.notifyAll();
    }

    synchronized public void workFinished(Source source){
        paused.remove(source);
        sourceMap.compute(source.readThreadId(),(tId, box)->{
            box.srcs.remove(source);
            return box.srcs.size()==0 ? null : box;
        });
        /*var sib = sourceMap.get(sourceInfo.readThreadId());
        sib.srcs.remove(sourceInfo);
        if (sib.len()==0) sourceMap.remove(sourceInfo.readThreadId());*/
        this.notifyAll();
    }



}
