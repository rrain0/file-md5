package md5.stage3read;

import md5.event.Event;
import md5.event.FileResultEv;
import md5.stage1sourcesdata.Source;


public class ReadEv extends Event<ReadEvType> implements FileResultEv<ReadEvType> {
    public final FilePart part;

    public ReadEv(ReadEvType type, FilePart part) {
        super(type);
        this.part = part;
    }

    @Override
    public ReadEvType getType() {
        return type;
    }

    @Override
    public Source getSource() {
        return part==null ? null : part.source();
    }
}
