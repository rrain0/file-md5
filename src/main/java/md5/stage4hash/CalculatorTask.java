package md5.stage4hash;

import md5.event.EventManager;
import md5.stage1sourcesdata.Source;
import md5.stage3read.FilePart;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CalculatorTask implements Runnable {
    private final MessageDigest mdEnc;
    private final Source source;
    private final Cache cache;
    private final CalculatorManager manager;
    private final EventManager eventManager;

    public CalculatorTask(Source source, CalculatorManager manager, Cache cache, EventManager eventManager) {
        this.source = source;
        this.manager = manager;
        this.cache = cache;
        this.eventManager = eventManager;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            //System.err.println("NoSuchAlgorithmException (MD5)");
            //e.printStackTrace();
            throw new RuntimeException("NoSuchAlgorithmException: MD5", e);
        }
    }

    private void addNextPart(byte[] bytes){
        mdEnc.update(bytes, 0, bytes.length);
    }

    /*private void addNextPart(byte[] bytes, int len){
        mdEnc.update(bytes, 0, len);
    }*/

    // get and reset
    public String getMd5(){
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        StringBuilder sb = new StringBuilder(md5);
        while (sb.length()<32) sb.insert(0, "0");
        return sb.toString().toUpperCase();
    }

    public void reset(){
        mdEnc.reset();
    }


    @Override
    public void run() {
        try {
            work();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void work() throws InterruptedException {
        while (true){
            //System.out.println("before await");
            manager.awaitForWork(source);
            //System.out.println("after await");
            FilePart part = cache.take(source);
            switch (part.info()){
                case NEW_FILE -> {}
                case PART -> addNextPart(part.part());
                case FILE_END -> {
                    String md5 = getMd5();

                    var result = new CalcResult(source, part.relPath(), CalcResult.Info.FILE_READY, md5);
                    //System.out.println(result);
                    eventManager.addEvent(new CalcEv(CalcEvType.FILE_CALCULATED, result));
                }
                case NOT_FOUND -> {
                    reset();
                }
                case READ_ERROR -> {
                    reset();
                }
                case SOURCE_FINISHED -> {
                    manager.workFinished(source);

                    //results.add(new ResultInfo(source, null, ResultInfo.Info.FINISH_ALL, null));
                    var result = new CalcResult(source, null, CalcResult.Info.SOURCE_READY, null);
                    //System.out.println(result);
                    eventManager.addEvent(new CalcEv(CalcEvType.SOURCE_READY, result));
                    return;
                }
            }
            manager.oneFilePartCalculated(source);
        }
    }



}
