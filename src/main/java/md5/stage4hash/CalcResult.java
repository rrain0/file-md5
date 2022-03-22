package md5.stage4hash;

import md5.stage1sourcesdata.Source;

import java.nio.file.Path;

public record CalcResult(
    Source source, Path srcPath, Path relPath, String md5
) { }
