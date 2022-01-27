package md5;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Calculator {
    private final MessageDigest mdEnc;

    public Md5Calculator() {
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            //System.err.println("NoSuchAlgorithmException (MD5)");
            //e.printStackTrace();
            throw new RuntimeException("NoSuchAlgorithmException (MD5)", e);
        }
    }

    public void addNextPart(byte[] bytes){
        mdEnc.update(bytes, 0, bytes.length);
    }

    public void addNextPart(byte[] bytes, int len){
        mdEnc.update(bytes, 0, len);
    }

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
}
