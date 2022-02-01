package md5;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5CalculatorTask implements Runnable {
    private final MessageDigest mdEnc;
    private final SourceInfo sourceInfo;
    private final Cache cache;
    private final Md5CalculatorManager manager;
    private final Md5Results results;

    public Md5CalculatorTask(SourceInfo sourceInfo, Md5CalculatorManager manager, Md5Results results, Cache cache) {
        this.sourceInfo = sourceInfo;
        this.manager = manager;
        this.results = results;
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
        sb.insert(24, " ");
        sb.insert(16, " ");
        sb.insert(8, " ");
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
            manager.awaitForWork(sourceInfo);
            FilePart part = cache.take(sourceInfo);
            switch (part.info){
                case NEW_FILE -> {}
                case PART -> addNextPart(part.part);
                case FILE_END -> {
                    String md5 = getMd5();
                    results.add(new ResultInfo(sourceInfo, part.relativePath, ResultInfo.Info.FILE, md5));
                }
                case NOT_FOUND -> {
                    reset();
                    results.add(new ResultInfo(sourceInfo, part.relativePath, ResultInfo.Info.NOT_FOUND, null));
                }
                case READ_ERROR -> {
                    reset();
                    results.add(new ResultInfo(sourceInfo, part.relativePath, ResultInfo.Info.READ_ERROR, null));
                }
                case FINISH_ALL -> {
                    manager.workFinished(sourceInfo);
                    results.add(new ResultInfo(sourceInfo, null, ResultInfo.Info.FINISH_ALL, null));
                    return;
                }
            }
            manager.oneFilePartCalculated(sourceInfo);
        }
    }



}
