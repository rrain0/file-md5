package md5;

import java.nio.file.Path;

public class FilePart {
    byte[] part;

    Info info;

    long from;
    long to;
    long len;

    SourceInfo sourceInfo;
    Path relativePath;

    public enum Info{
        NEW_FILE, PART, FILE_END,
        NOT_FOUND, READ_ERROR, FINISH_ALL
    }
}
