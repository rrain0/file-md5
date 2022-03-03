package md5.stage2estimate;

import md5.Event;
import md5.stage1sourcesdata.SourceInfo;

import java.io.*;
import java.nio.file.Path;


public class EstimateTask implements Runnable {
    public final SourceInfo src;
    public final EstimateManager manager;
    public final AllFiles allFiles;



    public EstimateTask(SourceInfo src, EstimateManager manager, AllFiles allFiles) {
        this.src = src;
        this.manager = manager;
        this.allFiles = allFiles;
    }


    @Override
    public void run() {
        try {
            walkFileTree(Path.of(""));
            manager.workFinished(src);
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
            allFiles.add(src, new FileInfo(p,f.length()));
        } else if (f.isDirectory()){
            for (File ff : f.listFiles()){
                Path pp = p.resolve(ff.getName());
                // можно ещё добавить event директория просмотрена
                walkFileTree(pp);
            }
        }
    }



}
