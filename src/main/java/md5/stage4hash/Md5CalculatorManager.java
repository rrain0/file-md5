package md5.stage4hash;

import md5.stage1sourcesdata.SourceInfo;
import md5.stage3read.Cache;
import md5.stage5results.Md5Results;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Md5CalculatorManager implements Runnable {


    private final Cache cache;
    private final int nSimultaneousThreads;
    private final Md5Results results;

    private final List<SourceInfo> onlineTasks;
    private int idx = 0;
    private final Set<SourceInfo> paused;

    public Md5CalculatorManager(List<SourceInfo> sources, Cache cache, int nSimultaneousThreads, Md5Results results) {
        onlineTasks = new ArrayList<>(sources);
        paused = new HashSet<>(sources);
        this.cache = cache;
        this.nSimultaneousThreads = nSimultaneousThreads;
        this.results = results;
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
        for (int i = 0; i < onlineTasks.size(); i++) {
            var source = onlineTasks.get(i);
            new Thread(new Md5CalculatorTask(source, this, results, cache)).start();
            if (onlineTasks.size()-paused.size() < nSimultaneousThreads) paused.remove(source);
        }
    }

    synchronized private void work() throws InterruptedException {
        while (!onlineTasks.isEmpty()){
            while (onlineTasks.size()-paused.size() < Integer.min(nSimultaneousThreads, onlineTasks.size())){
                idx = (idx+1)% onlineTasks.size();
                var source = onlineTasks.get(idx);
                paused.remove(source);
            }
            this.notifyAll();
            this.wait();
        }
    }

    synchronized public void awaitForWork(SourceInfo sourceInfo) throws InterruptedException {
        while (paused.contains(sourceInfo)) this.wait();
    }

    synchronized public void oneFilePartCalculated(SourceInfo sourceInfo){
        paused.add(sourceInfo);
        this.notifyAll();
    }

    synchronized public void workFinished(SourceInfo sourceInfo){
        paused.remove(sourceInfo);
        onlineTasks.remove(sourceInfo);
        notifyAll();
    }


}
