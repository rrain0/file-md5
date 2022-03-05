package md5.stage5results;

import md5.event.Event;
import md5.event.EventManager;
import md5.event.FileResultEv;
import md5.event.SubscriptionHolder;
import md5.stage1sourcesdata.Source;
import md5.stage1sourcesdata.SourceEv;
import md5.stage1sourcesdata.SourceEvType;
import md5.stage3read.ReadEv;
import md5.stage3read.ReadEvType;
import md5.stage4hash.CalcEv;
import md5.stage4hash.CalcEvType;
import md5.stage4hash.CalcResult;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

// todo поиск одинаковых файлов по:
//  относительный путь - уже сделано
//  просто название файла
//  по одинаковому хэшу (можно даже 2 хэша вычислять: MD5 и  CRC32 или SHA-256, можно ещё размер учитывать)

public class ResultManager implements Runnable {

    private final EventManager eventManager;
    private final BlockingQueue<Event<?>> incomeEvents = new LinkedBlockingQueue<>();
    private SubscriptionHolder holder;


    public List<Source> sources;

    // raw Map<sourcePath, ResultInfo with md5>
    private Map<Path, CalcResult> rawSourceToResultMap;

    private Set<Source> workingSources;


    // Map<relativePath, Map<sourcePath, ResultInfo with md5>>
    private final Map<Path, Map<Path, CalcResult>> results = new HashMap<>();
    // Map<srcPath, Map<type, count>>
    private Map<Path, Map<CalcResult.Info, Integer>> filesCnt;


    public ResultManager(EventManager eventManager) {
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
    public void run() {
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
                    sources = ev.sources;
                    workingSources = new HashSet<>(sources);
                    rawSourceToResultMap = sources.stream().collect(HashMap::new, (map,elem)->map.put(elem.path(),null), Map::putAll);
                    filesCnt = sources.stream().collect(Collectors.toUnmodifiableMap(Source::path, src->
                        Arrays.stream(CalcResult.Info.values())
                            .filter(info->info!= CalcResult.Info.SOURCE_READY)
                            .collect(Collectors.toMap(info->info,info->0))
                    ));

                    new Thread(this::work).start();
                }
                case FileResultEv ev && Set.of(
                    CalcEvType.FILE_CALCULATED,CalcEvType.SOURCE_READY,ReadEvType.READ_ERROR,ReadEvType.NOT_FOUND
                ).contains(ev.getType()) -> {
                    fileEvents.add(ev);
                }
                case CalcEv ev && ev.type==CalcEvType.ALL_READY -> {
                    unsubscribe();
                    break loop;
                }
                default -> {}
            }
        }
    }




    private final LinkedBlockingQueue<FileResultEv> fileEvents = new LinkedBlockingQueue<>();

    private void work() {
        try {
            while (true){
                var result = fileEvents.take();

                if (!sources.contains(result.getSource())) throw new RuntimeException("Unexpected source: "+result.getSource().path());

                // todo Exception if try to put result if it nonnull yet - this means 1 file proceeded 2+ times


                if (result.getType()==CalcEvType.SOURCE_READY){
                    workingSources.remove(result.getSource());
                    if (workingSources.isEmpty()) break;
                    else continue;
                }



                var sourceToResultMap = results.computeIfAbsent(
                    result.relativePath(), k->new HashMap<>(rawSourceToResultMap)
                );
                var srcPath = result.getSource().path();
                sourceToResultMap.put(srcPath, result);

                //filesCnt.get(srcPath).put(result.info(), filesCnt.get(srcPath).get(result.info())+1);
                filesCnt.get(srcPath).compute(result.info(), (info,cnt)->cnt+1);
            }
            finishAll();
            eventManager.addEvent(new ResultEv(ResultEvType.ALL_READY));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void finishAll(){
        var srcs = new ArrayList<>(this.sources); // копируем для того, чтобы можно было отсортировать если надо
        printResultsList(srcs);
        printFalsesResults(srcs);
        printFilesCnt(srcs);
    }

    // result может быть null если файла нет и не предполагалось
    private void printResultsList(List<Source> srcs){

        results.forEach((relPath,srcMap)->{
            System.out.println(relPath);

            String md5 = null;
            boolean equals = true;
            for (int i = 0; i < srcs.size(); i++) {
                var src = srcs.get(i);
                var result = srcMap.get(src.path());
                if (i==0) {
                    md5 = Optional.ofNullable(result).map(r->r.md5()).orElse(null);
                    equals &= md5!=null;
                }
                else equals &= Objects.equals(md5,Optional.ofNullable(result).map(r->r.md5()).orElse(null));
                System.out.println(printOne(src, relPath, result));
            }

            System.out.println("\t"+"EQUALS: "+equals);
        });
    }

    private void printFalsesResults(List<Source> srcs){
        System.out.println();
        System.out.println("FALSES:");
        results.forEach((relPath,srcMap)->{
            StringBuilder sb = new StringBuilder();
            sb.append(relPath).append('\n');

            String md5 = null;
            boolean equals = true;
            for (int i = 0; i < srcs.size(); i++) {
                var src = srcs.get(i);
                var result = srcMap.get(src.path());
                if (i==0) {
                    md5 = Optional.ofNullable(result).map(r->r.md5()).orElse(null);
                    equals &= md5!=null;
                }
                else equals &= Objects.equals(md5,Optional.ofNullable(result).map(r->r.md5()).orElse(null));
                sb.append(printOne(src, relPath, result)).append('\n');
            }

            sb.append("\t"+"EQUALS: "+equals);

            if (!equals) System.out.println(sb);
        });
    }

    private void printFilesCnt(List<Source> srcs){
        var infos = List.of(CalcResult.Info.FILE_READY, CalcResult.Info.READ_ERROR, CalcResult.Info.NOT_FOUND);
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append("FILES COUNT:").append('\n');
        for (var src : srcs) {
            sb.append('\t').append("src: [").append(src.readThreadId()).append(", ").append(src.path()).append("]").append(" ");
            for (var info : infos){
                sb.append(info).append(": ").append(filesCnt.get(src.path()).get(info)).append(" ");
            }
            sb.append('\n');
        }
        System.out.println(sb);
    }

    private String printOne(Source src, Path rel, CalcResult result){
        return (
            "\t"+
            "src: ["+src.readThreadId()+", "+src.path()+"] "+
            switch (Optional.ofNullable(result).map(r->r.info()).orElse(null)){
                case FILE_READY -> "MD5: "+result.md5();
                case NOT_FOUND -> "FILE NOT FOUND";
                case READ_ERROR -> "READ ERROR";
                case null -> "NO SUCH FILE";
                case default -> "???";
            }+" "+
            "full path: "+src.path().resolve(rel)
        );
    }

}
