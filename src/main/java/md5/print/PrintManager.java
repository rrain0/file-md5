package md5.print;

import md5.event.Event;
import md5.event.EventManager;
import md5.event.SubscriptionHolder;
import md5.stage1sourcesdata.Source;
import md5.stage1sourcesdata.SourceEv;
import md5.stage1sourcesdata.SourceEvType;
import md5.stage2estimate.EstimateEv;
import md5.stage2estimate.EstimateEvType;
import md5.stage3read.ReadEv;
import md5.stage3read.ReadEvType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PrintManager implements Runnable {

    private EventManager manager;
    private BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;

    public PrintManager(EventManager manager) {
        this.manager = manager;
        subscribe();
    }

    private void subscribe(){
        manager.subscribe(incomeEvents::put);
    }
    private void unsubscribe(){
        holder.unsubscribe();
        incomeEvents.clear();
    }




    private Map<Source,TotalInSource> sourceMap;


    @Override
    public void run() {
        try {
            while (true){
                Event<?> event = incomeEvents.take();
                switch (event){

                    case EstimateEv ev && ev.type==EstimateEvType.FILE_FOUND -> {
                        sourceMap.computeIfPresent(ev.fileInfo.src(), (s,ts)->{
                            ts.totalFiles++;
                            ts.totalSize+=ev.fileInfo.sz();
                            return ts;
                        });
                    }
                    case EstimateEv ev && ev.type==EstimateEvType.DIRECTORY_VIEWED -> {
                        sourceMap.get(ev.fileInfo.src()).totalFolders++;
                    }
                    case EstimateEv ev && ev.type==EstimateEvType.SOURCE_VIEWED -> {
                        System.out.println(sourceMap.get(ev.fileInfo.src()));
                    }
                    case SourceEv ev && ev.type==SourceEvType.ALL_READY -> {
                        sourceMap = ev.sources.stream().collect(HashMap::new, (map,s)->map.put(s,new TotalInSource(s)), Map::putAll);
                    }

                    case ReadEv ev && ev.type== ReadEvType.NEW_FILE -> {
                        System.out.println(String.format(
                            "Начинаю читать файл: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relativePath(), ev.part.source().tag()
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.PART -> {
                        System.out.println(String.format(
                            "Читаю файл: [%s]/%s #%s %s%%",
                            ev.part.source().path(), ev.part.relativePath(), ev.part.source().tag(),
                            Math.round(1d*ev.part.to()/ev.part.len()*100d)
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.FILE_END -> {
                        System.out.println(String.format(
                            "Файл прочитан: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relativePath(), ev.part.source().tag()
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.NOT_FOUND -> {
                        System.out.println(String.format(
                            "Файл не найден: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relativePath(), ev.part.source().tag()
                        ));
                    }
                    case ReadEv ev && ev.type== ReadEvType.READ_ERROR -> {
                        System.out.println(String.format(
                            "Ошибка чтения файла: [%s]/%s #%s",
                            ev.part.source().path(), ev.part.relativePath(), ev.part.source().tag()
                        ));
                    }

                    default -> {}
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
