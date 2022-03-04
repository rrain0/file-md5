package md5.stage4hash;

import md5.event.Event;
import md5.event.EventManager;
import md5.event.SubscriptionHolder;
import md5.stage1sourcesdata.Source;
import md5.stage1sourcesdata.SourceEv;
import md5.stage1sourcesdata.SourceEvType;
import md5.stage3read.ReadEv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CalculatorManager implements Runnable {

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    private final int nSimultaneousThreads;

    private volatile Cache cache;

    private volatile List<Source> onlineTasks;
    private int idx = 0;
    private volatile Set<Source> paused;

    public CalculatorManager(int nSimultaneousThreads, EventManager eventManager) {
        this.nSimultaneousThreads = nSimultaneousThreads;
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
                case SourceEv ev && ev.type==SourceEvType.ALL_READY -> {
                    cache = new Cache(ev.sources);
                    onlineTasks = new ArrayList<>(ev.sources);
                    paused = new HashSet<>(ev.sources);

                    for (int i = 0; i < onlineTasks.size(); i++) {
                        var source = onlineTasks.get(i);
                        new Thread(new CalculatorTask(source, this, cache)).start();
                        if (onlineTasks.size()-paused.size() < nSimultaneousThreads) paused.remove(source);
                    }

                    new Thread(this::work).start();
                }
                case ReadEv ev -> {
                    //System.out.println("wait for read event");
                    //System.out.println(ev.part);
                    cache.add(ev.part);
                }
                default -> {}
            }
        }


    }

    synchronized private void work() {
        try {
            while (!onlineTasks.isEmpty()){
                while (onlineTasks.size()-paused.size() < Integer.min(nSimultaneousThreads, onlineTasks.size())){
                    idx = (idx+1)% onlineTasks.size();
                    var source = onlineTasks.get(idx);
                    paused.remove(source);
                }
                this.notifyAll();
                this.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void awaitForWork(Source source) throws InterruptedException {
        while (paused.contains(source)) this.wait();
    }

    synchronized public void oneFilePartCalculated(Source source){
        paused.add(source);
        this.notifyAll();
    }

    synchronized public void workFinished(Source source){
        paused.remove(source);
        onlineTasks.remove(source);
        notifyAll();
    }


}
