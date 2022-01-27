package md5;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.StreamSupport;

public class Cache {
    //final long maxBytes;


    // todo:
    // Map<SourceInfo,Queue<FilePart>>
    // this is Map.of(source, new queue)

    // todo возможность зарезервировать место
    // наверное лучше запросить у кэша часть файла


    // блокирует поток
    /*public void reserve(long bytes){

    }
    public void free(long bytes){

    }*/


    private final Map<SourceInfo, BlockingQueue<FilePart>> map;

    public Cache(Iterable<SourceInfo> sourceInfos) {
        map = StreamSupport.stream(sourceInfos.spliterator(), false)
            .collect(HashMap::new, (map,info)->map.put(info, new LinkedBlockingQueue<FilePart>()), Map::putAll);
    }

    public void add(FilePart filePart) throws InterruptedException {
        map.get(filePart.sourceInfo).put(filePart);
    }
}
