package md5;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Md5Results {

    // Set<sourcePath>
    public final Set<String> sources;

    // Map<relativePath, Map<sourcePath, md5>>
    public final Map<String, Map<String, String>> results = Collections.synchronizedMap(new HashMap<>());


    public Md5Results(Set<String> sources) {
        this.sources = sources;
    }


    public void addMd5(String sourcePath, String relativePath, String md5){
        var sourceMap = results.putIfAbsent(
            relativePath,
            sources.stream().collect(()->Collections.synchronizedMap(new HashMap<>()), (map,elem)->map.put(elem,null), Map::putAll)
        );

        if (!sourceMap.containsKey(sourcePath)) throw new RuntimeException("Map doesn't contains source: "+sourcePath);

        sourceMap.put(sourcePath, md5);
    }

    // todo printResults or to file
}
