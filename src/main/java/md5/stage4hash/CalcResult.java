package md5.stage4hash;

import md5.stage1sourcesdata.Source;

import java.nio.file.Path;

public record CalcResult(
    Source source, Path relativePath, Info info, String md5
) {

    public enum Info {
        FILE_READY,
        SOURCE_READY
    }

}
