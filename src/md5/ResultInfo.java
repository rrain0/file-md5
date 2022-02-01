package md5;

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
