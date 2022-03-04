package md5.stage5results;

import md5.stage1sourcesdata.Source;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

// todo поиск одинаковых файлов по:
//  относительный путь - уже сделано
//  просто название файла
//  по одинаковому хэшу (можно даже 2 хэша вычислять: MD5 и  CRC32 или SHA-256, можно ещё размер учитывать)

public class Md5Results implements Runnable {

    // List<sourcePath>
    public final List<Source> sources;

    // raw Map<sourcePath, ResultInfo with md5>
    private final Map<Path, ResultInfo> rawSourceToResultMap;

    private final Set<Source> workingSources;


    // Map<relativePath, Map<sourcePath, ResultInfo with md5>>
    private final Map<Path, Map<Path, ResultInfo>> results = new HashMap<>();
    // Map<srcPath, Map<type, count>>
    private final Map<Path, Map<ResultInfo.Info, Integer>> filesCnt;


    public Md5Results(List<Source> sources) {
        this.sources = sources;
        workingSources = new HashSet<>(sources);
        rawSourceToResultMap = sources.stream().collect(HashMap::new, (map,elem)->map.put(elem.path(),null), Map::putAll);
        filesCnt = sources.stream().collect(Collectors.toUnmodifiableMap(src->src.path(), src->
            Arrays.stream(ResultInfo.Info.values())
                .filter(info->info!=ResultInfo.Info.FINISH_ALL)
                .collect(Collectors.toMap(info->info,info->0))
        ));
    }


    @Override
    public void run() {
        try {
            work();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final LinkedBlockingQueue<ResultInfo> queue = new LinkedBlockingQueue<>();

    private void work() throws InterruptedException {
        while (true){
            var result = queue.take();

            //System.out.println("AAAAA "+result);

            if (!sources.contains(result.source())) throw new RuntimeException("Unexpected source: "+result.source().path());

            // todo Exception if try to put result if it nonnull yet - this means 1 file proceeded 2+ times

            if (result.info()==ResultInfo.Info.FINISH_ALL){
                workingSources.remove(result.source());
                if (workingSources.isEmpty()) break;
                else continue;
            }



            var sourceToResultMap = results.computeIfAbsent(
                result.relativePath(), k->new HashMap<>(rawSourceToResultMap)
            );
            var srcPath = result.source().path();
            sourceToResultMap.put(srcPath, result);

            //filesCnt.get(srcPath).put(result.info(), filesCnt.get(srcPath).get(result.info())+1);
            filesCnt.get(srcPath).compute(result.info(), (info,cnt)->cnt+1);
        }
        finishAll();
    }

    public void add(ResultInfo result){
        queue.add(result);
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
        var infos = List.of(ResultInfo.Info.FILE, ResultInfo.Info.READ_ERROR, ResultInfo.Info.NOT_FOUND);
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

    private String printOne(Source src, Path rel, ResultInfo result){
        return (
            "\t"+
            "src: ["+src.readThreadId()+", "+src.path()+"] "+
            switch (Optional.ofNullable(result).map(r->r.info()).orElse(null)){
                case FILE -> "MD5: "+result.md5();
                case NOT_FOUND -> "FILE NOT FOUND";
                case READ_ERROR -> "READ ERROR";
                case null -> "NO SUCH FILE";
                case default -> "???";
            }+" "+
            "full path: "+src.path().resolve(rel)
        );
    }

}
