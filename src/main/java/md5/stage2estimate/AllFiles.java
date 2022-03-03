package md5.stage2estimate;

import md5.stage1sourcesdata.SourceInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AllFiles {
    private static class SourceFiles {
        public final SourceInfo src;
        public final List<FileInfo> relPaths = new ArrayList<>();
        public long totalSz = 0;
        public SourceFiles(SourceInfo src) { this.src = src; }
    }

    Map<SourceInfo, SourceFiles> files = new ConcurrentHashMap<>();

    public void add(SourceInfo src, FileInfo file){
        files.compute(src, (s,sFiles)->{
            if (sFiles==null) sFiles = new SourceFiles(src);
            sFiles.relPaths.add(file);
            sFiles.totalSz+=file.sz();
            return sFiles;
        });
    }

}
