package md5;

import java.util.*;
import java.util.stream.Collectors;

public class Md5CalculatorManager implements Runnable {


    public final Set<SourceInfo> sources;
    private final Cache cache;
    private final int nSimultaneousThreads;
    private final Md5Results results;

    private final List<SourceInfo> tasks;
    private int idx = 0;
    private final Set<SourceInfo> paused;

    public Md5CalculatorManager(Set<SourceInfo> sources, Cache cache, int nSimultaneousThreads, Md5Results results) {
        this.sources = sources;
        tasks = new ArrayList<>(sources);
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
        for (int i = 0; i < tasks.size(); i++) {
            var source = tasks.get(i);
            new Thread(new Md5CalculatorTask(source, this, results, cache)).start();
            if (tasks.size()-paused.size() < nSimultaneousThreads) paused.remove(source);
        }
    }

    synchronized private void work() throws InterruptedException {
        while (!tasks.isEmpty()){
            while (tasks.size()-paused.size() < Integer.min(nSimultaneousThreads,tasks.size())){
                idx = (idx+1)%tasks.size();
                var source = tasks.get(idx);
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
        tasks.remove(sourceInfo);
        notifyAll();
    }


}
