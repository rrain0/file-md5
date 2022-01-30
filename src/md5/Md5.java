package md5;

import java.util.*;
import java.util.stream.Collectors;

public class Md5 {

    public static void main(String[] args) throws InterruptedException {
        new Md5().run();
    }



    public final Set<SourceInfo> sources = Set.of(
        new SourceInfo("D:\\ТОРРЕНТЫ", "D:"),
        new SourceInfo("E:\\[test]", "E: 2.5\"")
    );

    private static class SourceInfoBox {
        int curr = 0;
        final List<SourceInfo> sourceInfoList;

        public SourceInfoBox(List<SourceInfo> sourceInfoList) {
            this.sourceInfoList = sourceInfoList;
        }

        int len(){ return sourceInfoList.size(); }
        SourceInfo get(int id){ return sourceInfoList.get(id); }
    }

    private final Map<Object, SourceInfoBox> sourceMap = sources.stream()
        .collect(Collectors.groupingBy(SourceInfo::threadId, Collectors.collectingAndThen(Collectors.toList(), SourceInfoBox::new)));

    private final Set<SourceInfo> paused = new HashSet<>(sources);

    //private final Object sync = new Object();

    public void run() throws InterruptedException {
        start();
        work();
    }

    synchronized private void start(){
        sourceMap.forEach((id,sib)->{
            var source = sib.get(0);
            new Thread(new FileReadTask(sib.get(0), , this)).start();
            paused.remove(source);
        });
    }

    synchronized private void work() throws InterruptedException {
        while (!sourceMap.isEmpty()){
            sourceMap.forEach((id,sib)->{
                if (paused.containsAll(sib.sourceInfoList)) {
                    sib.curr = (sib.curr+1) % sib.len();
                    paused.remove(sib.sourceInfoList.get(sib.curr));
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
        var sib = sourceMap.get(sourceInfo.threadId());
        sib.sourceInfoList.remove(sourceInfo);
        if (sib.len()==0) sourceMap.remove(sourceInfo.threadId());
        notifyAll();
    }
}
