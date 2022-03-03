package md5.stage2estimate;

import md5.Event;
import md5.stage1sourcesdata.SourceInfo;
import md5.stage3read.Cache;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// todo менеджер будет считать кто сколько файлов прочитал и переключать потоки
// todo менеджер будет пихать файлы в кэш

public class EstimateManager implements Runnable{

    private static class SourceInfoBox {
        int curr = 0;
        final List<SourceInfo> srcs;
        public SourceInfoBox(List<SourceInfo> srcs) {
            this.srcs = srcs;
        }
    }

    private final AllFiles allFiles;

    // Map<readThreadId, SourceInfoBox>
    private final Map<Object, SourceInfoBox> sourceMap;
    private final Set<SourceInfo> paused;

    public EstimateManager(List<SourceInfo> sources, AllFiles allFiles) {
        sourceMap = sources.stream().collect(Collectors.groupingBy(
                SourceInfo::readThreadId,
                Collectors.collectingAndThen(Collectors.toList(), SourceInfoBox::new)
            ));
        paused = new HashSet<>(sources);
        this.allFiles = allFiles;
    }

    public void run(){
        try {
            start();
            work();
            // todo notify all work finished - make event
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized private void start(){
        sourceMap.forEach((id,box)->{
            for (int i = 0; i < box.srcs.size(); i++) {
                var source = box.srcs.get(i);
                new Thread(new EstimateTask(box.srcs.get(i), this, allFiles)).start();
                if (i==0) paused.remove(source);
            }
        });
    }

    synchronized private void work() throws InterruptedException {
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
    }

    synchronized public void awaitForWork(SourceInfo sourceInfo) throws InterruptedException {
        while (paused.contains(sourceInfo)) this.wait();
    }


    synchronized public void fileFound(SourceInfo sourceInfo){
        paused.add(sourceInfo);
        this.notifyAll();
    }

    synchronized public void workFinished(SourceInfo sourceInfo){
        paused.remove(sourceInfo);
        sourceMap.compute(sourceInfo.readThreadId(),(tId,box)->{
            box.srcs.remove(sourceInfo);
            return box.srcs.size()==0 ? null : box;
        });
        /*var sib = sourceMap.get(sourceInfo.readThreadId());
        sib.srcs.remove(sourceInfo);
        if (sib.len()==0) sourceMap.remove(sourceInfo.readThreadId());*/
        this.notifyAll();
    }



}
