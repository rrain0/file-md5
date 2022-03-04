package md5.stage3read;

import md5.stage1sourcesdata.Source;
import md5.stage2estimate.FileInfo;

import java.util.ArrayList;
import java.util.List;

public class SourceFiles {
    public final Source src;
    public final List<FileInfo> files = new ArrayList<>();

    public SourceFiles(Source src) {
        this.src = src;
    }
}
