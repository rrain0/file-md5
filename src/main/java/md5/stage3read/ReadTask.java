package md5.stage3read;

import md5.event.EventManager;

import java.io.*;
import java.nio.file.Path;


// todo notify thread manager about progress
// todo set max filepart in ram

public class ReadTask implements Runnable {
    private final SourceFiles files;
    private final ReadManager readManager;
    private final EventManager eventManager;




    public ReadTask(SourceFiles files, ReadManager readManager, EventManager eventManager) {
        this.files = files;
        this.readManager = readManager;
        this.eventManager = eventManager;
    }


    @Override
    public void run() {
        try {
            for (var info : files.files){
                readManager.awaitForWork(files);
                File f = info.src().path().resolve(info.relPath()).toFile();
                readFile(f,info.relPath());
                readManager.oneFileWasRead(files);
            }
            readManager.workFinished(files);
            FilePart fp = FilePart.builder()
                .source(files.src)
                .info(FilePart.Info.SOURCE_FINISHED)
                .build();
            eventManager.addEvent(new ReadEv(ReadEvType.SOURCE_FINISHED, fp));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void readFile(File f, Path relativePath) throws InterruptedException {
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))){
            final long len = f.length();
            final int chunkSz = 50*1024*1024; // размер считываемого за раз куска в байтах

            {
                FilePart fp = FilePart.builder()
                    .source(files.src)
                    .relPath(relativePath)
                    .info(FilePart.Info.NEW_FILE)
                    .len(len)
                    .build();
                eventManager.addEvent(new ReadEv(ReadEvType.NEW_FILE, fp));
            }

            for (long from = 0, to = 0; to<len; from=to){
                to = Long.min(from+chunkSz,len);
                byte[] buf = new byte[(int) (to-from)];
                bis.read(buf);

                FilePart fp = FilePart.builder()
                    .source(files.src)
                    .relPath(relativePath)
                    .info(FilePart.Info.PART)
                    .from(from)
                    .to(to)
                    .len(len)
                    .part(buf)
                    .build();
                eventManager.addEvent(new ReadEv(ReadEvType.PART, fp));
            }

            {
                FilePart fp = FilePart.builder()
                    .source(files.src)
                    .relPath(relativePath)
                    .info(FilePart.Info.FILE_END)
                    .len(len)
                    .build();
                eventManager.addEvent(new ReadEv(ReadEvType.FILE_END, fp));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            FilePart fp = FilePart.builder()
                .source(files.src)
                .relPath(relativePath)
                .info(FilePart.Info.NOT_FOUND)
                .build();
            eventManager.addEvent(new ReadEv(ReadEvType.NOT_FOUND, fp));
        } catch (IOException e) {
            e.printStackTrace();

            FilePart fp = FilePart.builder()
                .source(files.src)
                .relPath(relativePath)
                .info(FilePart.Info.READ_ERROR)
                .build();
            eventManager.addEvent(new ReadEv(ReadEvType.READ_ERROR, fp));
        }
    }

}
