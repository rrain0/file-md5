package md5.stage2estimate;

import md5.stage1sourcesdata.Source;

import java.nio.file.Path;

public record FileInfo(Source src, Path relPath, Long sz){}
