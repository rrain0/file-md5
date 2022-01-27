package md5;

import java.nio.file.Path;

public record SourceInfo(
    Path path,
    Object threadId // потоки с разным id будут выполняться параллельно (одновременное чтение с разных физических дисков или с одного ссд)
){
    public SourceInfo(String path, Object threadId) {
        this(Path.of(path),threadId);
    }
}
