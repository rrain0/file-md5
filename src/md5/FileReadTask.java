package md5;

import java.io.*;
import java.nio.file.Path;

public class FileReadTask implements Runnable {
    public final SourceInfo sourceInfo;
    public final Cache cache;


    // todo notify thread manager about progress
    // todo max filepart in ram


    public FileReadTask(SourceInfo sourceInfo, Cache cache) {
        this.sourceInfo = sourceInfo;
        this.cache = cache;
    }


    @Override
    public void run() {
        try {
            walkFileTree(Path.of(""));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void walkFileTree(Path p) throws InterruptedException {
        Path root = sourceInfo.path();
        File f = root.resolve(p).toFile();
        if (f.isFile()){
            readFile(f,p);
        } else if (f.isDirectory()){
            for (File ff : f.listFiles()){
                Path pp = p.resolve(ff.getName());
                walkFileTree(pp);
            }
        }
    }

    private void readFile(File f, Path relativePath) throws InterruptedException {
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))){
            final long len = f.length();
            final int chunkSz = 50*1024*1024; // размер считываемого за раз куска в байтах

            for (long from = 0, to;;){
                to = Long.min(from+chunkSz,len);
                byte[] buf = new byte[(int) (to-from)];
                bis.read(buf);
                FilePart fp = new FilePart();
                fp.sourceInfo = sourceInfo;
                fp.relativePath = relativePath;
                fp.info = to<len ? FilePart.Info.PART : FilePart.Info.LAST;
                fp.from = from;
                fp.to = to;
                fp.len = len;
                fp.part = buf;
                cache.add(fp);
                if (fp.info == FilePart.Info.LAST) break;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            FilePart fp = new FilePart();
            fp.sourceInfo = sourceInfo;
            fp.relativePath = relativePath;
            fp.info = FilePart.Info.NOT_FOUND;
            cache.add(fp);
        } catch (IOException e) {
            e.printStackTrace();
            FilePart fp = new FilePart();
            fp.sourceInfo = sourceInfo;
            fp.relativePath = relativePath;
            fp.info = FilePart.Info.READ_ERROR;
            cache.add(fp);
        }
    }

}
