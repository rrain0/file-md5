package md5;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Md5 {

    public static void main(String[] args) {
        new Md5().run();
    }



    public final Set<SourceInfo> paths = Set.of(
        new SourceInfo("D:\\ТОРРЕНТЫ", "D:"),
        new SourceInfo("E:\\[test]", "E: 2.5\"")
    );

    private final Map<Object, List<SourceInfo>> sourceMap = paths.stream()
        .collect(Collectors.groupingBy(SourceInfo::threadId));

    private final Set<SourceInfo> paused = new HashSet<>(paths);

    public void run(){

    }

    public boolean canWork(SourceInfo sourceInfo){
        return !paused.contains(sourceInfo);
    }

}
