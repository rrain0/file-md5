package md5;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FileReadManager implements Runnable{

    private static class SourceInfoBox {
        int curr = 0;
        final List<SourceInfo> sourceInfoList;

        public SourceInfoBox(List<SourceInfo> sourceInfoList) {
            this.sourceInfoList = sourceInfoList;
        }

        int len(){ return sourceInfoList.size(); }
        SourceInfo get(int id){ return sourceInfoList.get(id); }
    }

    private final Cache cache;

    public final Set<SourceInfo> sources;
    // Map<threadId, SourceInfoBox>
    private final Map<Object, SourceInfoBox> sourceMap;
    private final Set<SourceInfo> paused;

    public FileReadManager(Set<SourceInfo> sources, Cache cache) {
        this.sources = sources;
        sourceMap = sources.stream()
            .collect(Collectors.groupingBy(SourceInfo::threadId, Collectors.collectingAndThen(Collectors.toList(), SourceInfoBox::new)));
        paused = new HashSet<>(sources);
        this.cache = cache;
    }


    public void run(){
        try {
            start();
            work();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized private void start(){
        sourceMap.forEach((id,sib)->{
            for (int i = 0; i < sib.len(); i++) {
                var source = sib.get(i);
                new Thread(new FileReadTask(sib.get(i), cache, this)).start();
                if (i==0) paused.remove(source);
            }

        });
    }

    synchronized private void work() throws InterruptedException {
        while (!sourceMap.isEmpty()){
            sourceMap.forEach((id,sib)->{
                if (paused.containsAll(sib.sourceInfoList)) {
                    sib.curr = (sib.curr+1) % sib.len();
                    paused.remove(sib.get(sib.curr));
                }
            });
            //System.out.println("continue to work");
            this.notifyAll();
            this.wait();
        }
    }

    synchronized public void awaitForWork(SourceInfo sourceInfo) throws InterruptedException {
        //System.out.println("await for work "+sourceInfo.path());
        while (paused.contains(sourceInfo)) this.wait();
        //System.out.println("await for work finished "+sourceInfo.path());
    }

    synchronized public void oneFileWasRead(SourceInfo sourceInfo){
        //System.out.println("file readed "+sourceInfo.path());

        paused.add(sourceInfo);
        this.notifyAll();
    }

    synchronized public void workFinished(SourceInfo sourceInfo){
        //System.out.println("work finished "+sourceInfo.path());

        paused.remove(sourceInfo);
        var sib = sourceMap.get(sourceInfo.threadId());
        sib.sourceInfoList.remove(sourceInfo);
        if (sib.len()==0) sourceMap.remove(sourceInfo.threadId());
        this.notifyAll();
    }


}
