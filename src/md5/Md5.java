package md5;

import java.util.*;

/*
    todo
     задавать конфиг в json рядом с исполняемым или jar файлом
     <имя проекта>.<профиль>.json
     file-md5.config.json
     file-md5.default.config.json
     file-md5.profile1.config.json
 */

// todo made a parameter sequential with <thread>

// todo надо сделать 1 диск - 1 одновременный поток, но перед этим проверить на ссд как он читает, есть ли разница, если 2 потока или 1

public class Md5 {

    public static void main(String[] args) throws InterruptedException {

        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("D:\\ТОРРЕНТЫ", "D:"),
            new SourceInfo("E:\\[test]", "E: 2.5\"")
        );*/
        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("I:\\test\\1", "I"),
            new SourceInfo("I:\\test\\2", "I"),
            new SourceInfo("F:\\test\\3", "F")
        );*/
        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("K:\\GAMES\\GAMES 2\\Streets of Rage\\Street Of Rage Collection\\Streets_of_Rage_2X_v1.1_setup.exe", "1")
        );*//*
        final Set<SourceInfo> sources = Set.of(
            new SourceInfo("L:\\[удалить]\\Street Of Rage Collection", "T64"),
            new SourceInfo("G:\\[удалить]\\Street Of Rage Collection", "DATA_TWO"),
            new SourceInfo("K:\\GAMES\\GAMES 2\\Streets of Rage\\Street Of Rage Collection", "Seagate")
        );*/
        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("H:\\[test]\\Fallout 4", "WD 2.5"),
            new SourceInfo("K:\\[test]\\[долгий ящик]\\Fallout 4", "Seagate")
        );*/
        final Set<SourceInfo> sources = Set.of(
            new SourceInfo("H:\\GAMES\\Grand Theft Auto V by xatab", "WD 2.5"),
            new SourceInfo("K:\\GAMES\\GAMES 3\\Grand Theft Auto V by xatab", "Seagate")
        );



        final int nSimultaneousThreads = 4;

        Cache cache = new Cache(sources);
        new Thread(new FileReadManager(sources, cache)).start();
        Md5Results results = new Md5Results(sources);
        new Thread(results).start();
        new Thread(new Md5CalculatorManager(sources, cache, nSimultaneousThreads, results)).start();

    }




}
