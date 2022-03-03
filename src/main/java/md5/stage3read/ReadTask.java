package md5.stage3read;

import md5.stage1sourcesdata.SourceInfo;

import java.io.*;
import java.nio.file.Path;


// todo notify thread manager about progress
// todo set max filepart in ram

public class ReadTask implements Runnable {
    public final SourceInfo sourceInfo;
    public final Cache cache;
    public final ReadManager readManager;




    public ReadTask(SourceInfo sourceInfo, Cache cache, ReadManager readManager) {
        this.sourceInfo = sourceInfo;
        this.cache = cache;
        this.readManager = readManager;
    }


    @Override
    public void run() {
        try {
            walkFileTree(Path.of(""));
            readManager.workFinished(sourceInfo);
            FilePart fp = FilePart.builder()
                .sourceInfo(sourceInfo)
                .info(FilePart.Info.FINISH_ALL)
                .build();
            cache.add(fp);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void walkFileTree(Path p) throws InterruptedException {
        readManager.awaitForWork(sourceInfo);
        Path root = sourceInfo.path();
        File f = root.resolve(p).toFile();
        if (f.isFile()){
            readFile(f,p);
            readManager.oneFileWasRead(sourceInfo);
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

            {
                FilePart fp = FilePart.builder()
                    .sourceInfo(sourceInfo)
                    .relativePath(relativePath)
                    .info(FilePart.Info.NEW_FILE)
                    .len(len)
                    .build();
                cache.add(fp);
            }

            //System.out.println("before for");
            for (long from = 0, to = 0; to<len; from=to){
                to = Long.min(from+chunkSz,len);
                byte[] buf = new byte[(int) (to-from)];
                bis.read(buf);

                FilePart fp = FilePart.builder()
                    .sourceInfo(sourceInfo)
                    .relativePath(relativePath)
                    .info(FilePart.Info.PART)
                    .from(from)
                    .to(to)
                    .len(len)
                    .part(buf)
                    .build();
                cache.add(fp);
            }
            //System.out.println("after for");

            {
                FilePart fp = FilePart.builder()
                    .sourceInfo(sourceInfo)
                    .relativePath(relativePath)
                    .info(FilePart.Info.FILE_END)
                    .len(len)
                    .build();
                cache.add(fp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            FilePart fp = FilePart.builder()
                .sourceInfo(sourceInfo)
                .relativePath(relativePath)
                .info(FilePart.Info.NOT_FOUND)
                .build();
            cache.add(fp);
        } catch (IOException e) {
            e.printStackTrace();

            FilePart fp = FilePart.builder()
                .sourceInfo(sourceInfo)
                .relativePath(relativePath)
                .info(FilePart.Info.READ_ERROR)
                .build();
            cache.add(fp);
        }
    }

}
