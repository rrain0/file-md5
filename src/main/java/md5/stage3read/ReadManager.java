package md5.stage3read;

import md5.stage1sourcesdata.SourceInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReadManager implements Runnable{

    private static class SourceInfoBox {
        int curr = 0;
        final List<SourceInfo> srcs;
        public SourceInfoBox(List<SourceInfo> srcs) {
            this.srcs = srcs;
        }

    }

    private final Cache cache;
    // Map<readThreadId, SourceInfoBox>
    private final Map<Object, SourceInfoBox> sourceMap;
    private final Set<SourceInfo> paused;

    public ReadManager(List<SourceInfo> sources, Cache cache) {
        sourceMap = sources.stream().collect(Collectors.groupingBy(
            SourceInfo::readThreadId, Collectors.collectingAndThen(Collectors.toList(), SourceInfoBox::new)
        ));
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
            for (int i = 0; i < sib.srcs.size(); i++) {
                var source = sib.srcs.get(i);
                new Thread(new ReadTask(sib.srcs.get(i), cache, this)).start();
                if (i==0) paused.remove(source);
            }

        });
    }

    synchronized private void work() throws InterruptedException {
        while (!sourceMap.isEmpty()){
            sourceMap.forEach((id,sib)->{
                if (paused.containsAll(sib.srcs)) {
                    sib.curr = (sib.curr+1) % sib.srcs.size();
                    paused.remove(sib.srcs.get(sib.curr));
                }
            });
            this.notifyAll();
            this.wait();
        }
    }

    synchronized public void awaitForWork(SourceInfo sourceInfo) throws InterruptedException {
        while (paused.contains(sourceInfo)) this.wait();
    }

    synchronized public void oneFileWasRead(SourceInfo sourceInfo){
        paused.add(sourceInfo);
        this.notifyAll();
    }

    synchronized public void workFinished(SourceInfo sourceInfo){
        paused.remove(sourceInfo);
        sourceMap.compute(sourceInfo.readThreadId(),(tId,box)->{
            box.srcs.remove(sourceInfo);
            return box.srcs.size()==0 ? null : box;
        });
        /*var box = sourceMap.get(sourceInfo.readThreadId());
        box.srcs.remove(sourceInfo);
        if (box.srcs.size()==0) sourceMap.remove(sourceInfo.readThreadId());*/
        this.notifyAll();
    }


}
