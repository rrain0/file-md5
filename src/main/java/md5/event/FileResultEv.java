package md5.event;

import md5.stage1sourcesdata.Source;

public interface FileResultEv<T> {
    T getType();
    Source getSource();
}
