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
        FIRST, PART, LAST, NOT_FOUND, READ_ERROR
    }
}
