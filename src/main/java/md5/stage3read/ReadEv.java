package md5.stage3read;

import md5.event.Event;


public class ReadEv extends Event<ReadEvType> {
    public final FilePart part;

    public ReadEv(ReadEvType type, FilePart part) {
        super(type);
        this.part = part;
    }

}
