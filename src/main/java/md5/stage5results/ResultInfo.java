package md5.stage5results;

import md5.stage1sourcesdata.SourceInfo;

import java.nio.file.Path;

public record ResultInfo(
    SourceInfo sourceInfo, Path relativePath, Info info, String md5
) {

    public enum Info {
        FILE,
        NOT_FOUND, READ_ERROR,
        FINISH_ALL
    }

}
