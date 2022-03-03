package md5.stage3read;

import lombok.Builder;
import md5.stage1sourcesdata.SourceInfo;

import java.nio.file.Path;

// todo apply builder pattern

public record FilePart(
    byte[] part,

    Info info,

    long from,
    long to,
    long len,

    SourceInfo sourceInfo,
    Path relativePath
){
    public enum Info{
        NEW_FILE, PART, FILE_END,
        NOT_FOUND, READ_ERROR, FINISH_ALL
    }

    @Builder
    public FilePart {}
}
