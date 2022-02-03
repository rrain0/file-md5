package md5;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

// todo ограничить размер задавая суммарный максимальный размер данных файлов (т.е. учитывать только byte[] файла)

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
            .collect(Collectors.toUnmodifiableMap(info->info, info->new LinkedBlockingQueue<>()));
    }

    public void add(FilePart filePart) throws InterruptedException {
        map.get(filePart.sourceInfo).put(filePart);
    }

    public FilePart take(SourceInfo source) throws InterruptedException {
        return map.get(source).take();
    }
}
