package md5;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Md5Results {

    // Set<sourcePath>
    public final Set<SourceInfo> sources;

    // Map<relativePath, Map<sourcePath, ResultInfo with md5>>
    private final Map<Path, Map<Path, ResultInfo>> results = new HashMap<>();

    // raw Map<sourcePath, ResultInfo with md5>
    private final Map<Path, ResultInfo> rawSourceToResultMap;

    private final Set<SourceInfo> workingSources;


    public Md5Results(Set<SourceInfo> sources) {
        this.sources = sources;
        workingSources = new HashSet<>(sources);
        rawSourceToResultMap = sources.stream().collect(HashMap::new, (map,elem)->map.put(elem.path(),null), Map::putAll);
    }


    synchronized public void add(ResultInfo result){
        if (!sources.contains(result.sourceInfo())) throw new RuntimeException("Unexpected source: "+result.sourceInfo().path());

        System.out.println(result);

        if (result.info()==ResultInfo.Info.FINISH_ALL){
            workingSources.remove(result.sourceInfo());
            if (workingSources.isEmpty()) finishAll();
            return;
        }

        var sourceToResultMap = results.computeIfAbsent(
            result.relativePath(), k->new HashMap<>(rawSourceToResultMap)
        );

        sourceToResultMap.put(result.sourceInfo().path(), result);
    }

    synchronized private void finishAll(){
        printResults();
    }

    private void printResults(){
        var sources = new ArrayList<>(this.sources);
        results.forEach((rel,srcMap)->{
            System.out.println(rel);

            var first = srcMap.get(sources.get(0).path());
            System.out.println("\t"+first.sourceInfo().path()+" MD5: "+first.md5());
            String md5 = first.md5();
            boolean equals = true;
            for (int i = 1; i < sources.size(); i++) {
                var result = srcMap.get(sources.get(i).path());
                System.out.println("\t"+result.sourceInfo().path()+" MD5: "+result.md5());
                equals &= md5.equals(result.md5());
            }
            System.out.println("\t"+"EQUALS: "+equals);
        });
    }

}
