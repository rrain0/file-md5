package md5.stage5results;

import md5.event.Event;

public class ResultEv extends Event<ResultEvType> {

    public ResultEv(ResultEvType type) {
        super(type);
    }
}
