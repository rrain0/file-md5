package md5.stage5results;

import md5.stage1sourcesdata.Source;

import java.nio.file.Path;

public record ResultInfo(
    Source source, Path relativePath, Info info, String md5
) {

    public enum Info {
        FILE,
        NOT_FOUND, READ_ERROR,
        FINISH_ALL
    }

}
