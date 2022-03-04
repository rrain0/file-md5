package md5.stage4hash;

import md5.stage1sourcesdata.Source;
import md5.stage3read.FilePart;
import md5.stage5results.Md5Results;
import md5.stage5results.ResultInfo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CalculatorTask implements Runnable {
    private final MessageDigest mdEnc;
    private final Source source;
    private final Cache cache;
    private final CalculatorManager manager;

    public CalculatorTask(Source source, CalculatorManager manager, Cache cache) {
        this.source = source;
        this.manager = manager;
        this.cache = cache;
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

                    //results.add(new ResultInfo(source, part.relativePath(), ResultInfo.Info.FILE, md5));
                    System.out.println(new ResultInfo(source, part.relativePath(), ResultInfo.Info.FILE, md5));
                }
                case NOT_FOUND -> {
                    reset();

                    //results.add(new ResultInfo(source, part.relativePath(), ResultInfo.Info.NOT_FOUND, null));
                    System.out.println(new ResultInfo(source, part.relativePath(), ResultInfo.Info.NOT_FOUND, null));
                }
                case READ_ERROR -> {
                    reset();

                    //results.add(new ResultInfo(source, part.relativePath(), ResultInfo.Info.READ_ERROR, null));
                    System.out.println(new ResultInfo(source, part.relativePath(), ResultInfo.Info.READ_ERROR, null));
                }
                case SOURCE_FINISHED -> {
                    manager.workFinished(source);

                    //results.add(new ResultInfo(source, null, ResultInfo.Info.FINISH_ALL, null));
                    System.out.println(new ResultInfo(source, null, ResultInfo.Info.FINISH_ALL, null));

                    return;
                }
            }
            manager.oneFilePartCalculated(source);
        }
    }



}
