package md5.stage3read;

import lombok.Builder;
import md5.stage1sourcesdata.Source;

import java.nio.file.Path;

// todo apply builder pattern

public record FilePart(
    byte[] part,

    Info info,

    long from,
    long to,
    long len,

    Source source,
    Path relPath
){
    public enum Info{
        NEW_FILE, PART, FILE_END,
        NOT_FOUND, READ_ERROR, SOURCE_FINISHED
    }

    @Builder
    public FilePart {}
}
