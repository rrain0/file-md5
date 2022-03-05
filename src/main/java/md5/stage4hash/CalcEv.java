package md5.stage4hash;

import md5.event.Event;
import md5.event.FileResultEv;
import md5.stage1sourcesdata.Source;

public class CalcEv extends Event<CalcEvType> implements FileResultEv<CalcEvType> {
    public final CalcResult result;

    public CalcEv(CalcEvType type, CalcResult result) {
        super(type);
        this.result = result;
    }

    @Override
    public CalcEvType getType() {
        return type;
    }

    @Override
    public Source getSource() {
        return result==null ? null : result.source();
    }
}
