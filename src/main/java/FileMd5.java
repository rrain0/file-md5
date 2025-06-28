package main.java;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FileMd5 {
  private static final String PATH_1 =
    "M:\\from Angarsk old PC\\D\\ТОРРЕНТЫ\\JoJo's Bizarre Adventure S05";
  private static final String PATH_2 =
    "M:\\Anime\\JoJo\\JoJo's Bizarre Adventure S05 Golden Wind Anidub";


  private static int total = 0;
  private static int falses = 0;

  public static void main(String[] args) {
    calcOld();
  }


  public static void calcOld() {
    try {
      recursive(new File(PATH_1), new File(PATH_2));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      System.out.println();
      System.out.println("TOTAL: " + total);
      System.out.println("FALSES: " + falses);
    }
  }

  private static void recursive(File f1, File f2) throws Exception {
    if (f1.isDirectory()) {
      for(File ff : f1.listFiles()) {
        recursive(ff, new File(f2, ff.getName()));
      }
    }
    else if (f1.isFile()) {
      String md5f1 = getMd5(f1);
      String md5f2 = getMd5(f2);
      boolean equality = md5f1.equals(md5f2);
      total++;
      if (!equality) falses++;
      String eq = equality ? "true" : "FALSE";
      String md5 = equality ? " MD5: " + md5f1 : "";
      System.out.println(eq + " file: " + f1.getAbsolutePath() + md5);
    }
  }

  private static final MD5Calculator MD5 = new MD5Calculator();

  private static String getMd5(File f) throws IOException {
    var maxArrLen = Integer.MAX_VALUE / 2;
    var len = f.length();
    try (InputStream is = new FileInputStream(f)) {
      MD5.reset();
      while (len > 0) {
        var partLen = (int)Math.min(len, maxArrLen);
        len -= partLen;
        var part = new byte[partLen];
        is.read(part);
        MD5.nextPart(part);
      }
      return MD5.getMD5();
    }
  }



  private static class MD5Calculator {
    private final MessageDigest mdEnc;

    public MD5Calculator() {
      try {
        mdEnc = MessageDigest.getInstance("MD5");
      }
      catch (NoSuchAlgorithmException e) {
        System.out.println("NoSuchAlgorithmException (MD5)");
        e.printStackTrace();
        throw new RuntimeException();
      }
    }

    public void nextPart(byte[] bytes) {
      mdEnc.update(bytes, 0, bytes.length);
    }

    // get and reset
    public String getMD5() {
      String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
      StringBuilder sb = new StringBuilder(md5);
      while (sb.length() < 32) sb.insert(0, "0");
      sb.insert(24, " ");
      sb.insert(16, " ");
      sb.insert(8, " ");
      return sb.toString().toUpperCase();
    }

    public void reset() {
      mdEnc.reset();
    }
  }
}
