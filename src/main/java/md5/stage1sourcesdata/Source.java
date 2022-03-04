package md5.stage1sourcesdata;

import lombok.Builder;

import java.nio.file.Path;
import java.util.Optional;

public record Source(
    Path path,
    Object readThreadId,// потоки с разным id будут выполняться параллельно (одновременное чтение с разных физических дисков или с одного ссд)
    Object tag
){
    @Builder
    public Source(String path, Object readThreadId, Object tag) {
        this(Path.of(path), Optional.ofNullable(readThreadId).orElse(new Id()), tag);
    }
}
