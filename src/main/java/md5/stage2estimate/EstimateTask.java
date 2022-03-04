package md5.stage2estimate;

import md5.event.EventManager;
import md5.stage1sourcesdata.Source;

import java.io.*;
import java.nio.file.Path;


public class EstimateTask implements Runnable {
    private final Source src;
    private final EstimateManager manager;
    private final EventManager eventManager;


    public EstimateTask(Source src, EstimateManager manager, EventManager eventManager) {
        this.src = src;
        this.manager = manager;
        this.eventManager = eventManager;
    }


    @Override
    public void run() {
        try {
            walkFileTree(Path.of(""));
            manager.workFinished(src);
            eventManager.addEvent(new EstimateEv(EstimateEvType.SOURCE_VIEWED, new FileInfo(src, null, null)));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void walkFileTree(Path p) throws InterruptedException {
        manager.awaitForWork(src);
        Path root = src.path();
        File f = root.resolve(p).toFile();
        if (f.isFile()){
            manager.fileFound(src);
            eventManager.addEvent(new EstimateEv(EstimateEvType.FILE_FOUND, new FileInfo(src,p,f.length())));
        } else if (f.isDirectory()){
            for (File ff : f.listFiles()){
                Path pp = p.resolve(ff.getName());
                walkFileTree(pp);
            }
            eventManager.addEvent(new EstimateEv(EstimateEvType.DIRECTORY_VIEWED, new FileInfo(src, p, null)));
        }
    }



}
