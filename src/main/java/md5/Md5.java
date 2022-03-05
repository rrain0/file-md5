package md5;

import md5.event.EventManager;
import md5.print.PrintManager;
import md5.stage1sourcesdata.Source;
import md5.stage1sourcesdata.SourceManager;
import md5.stage2estimate.EstimateManager;
import md5.stage3read.ReadManager;
import md5.stage4hash.CalculatorManager;
import md5.stage5results.ResultManager;

import java.util.List;

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

    public static void main(String[] args) {
        // Если расположения находятся на одном физическом диске, то лучше их указать в одном потоке чтения,
        // чтобы программа их читала последовательно, а не параллельно



        final List<Source> sources = List.of(
            Source.builder().path("L:\\ВИДЕО\\Dr. Stone - Доктор Стоун\\Доктор Стоун S01 1080p HEVC AniPlague")
                .tag(1).readThreadId("L").build(),
            Source.builder().path("E:\\ТОРРЕНТЫ\\Доктор Стоун S01 1080p HEVC")
                .tag(2).readThreadId("E").build()
        );
        /*final List<Source> sources = List.of(
            Source.builder().path("E:\\1233").tag(1).readThreadId("E").build(),
            Source.builder().path("E:\\1234").tag(2).readThreadId("E").build()
        );*/

        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("D:\\ТОРРЕНТЫ", "D:"),
            new SourceInfo("E:\\[test]", "E: 2.5\"")
        );*/

        /*final List<Source> sources = List.of(
            Source.builder().path("I:\\test\\1").tag(1).readThreadId("I").build(),
            Source.builder().path("I:\\test\\2").tag(2).readThreadId("I").build(),
            Source.builder().path("F:\\test\\3").tag(3).readThreadId("F").build()
            *//*new SourceInfo("I:\\test\\1", "I"),
            new SourceInfo("I:\\test\\2", "I"),
            new SourceInfo("F:\\test\\3", "F")*//*
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
        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("H:\\GAMES\\Grand Theft Auto V by xatab", "WD 2.5"),
            new SourceInfo("K:\\GAMES\\GAMES 3\\Grand Theft Auto V by xatab", "Seagate")
        );*/


/*

        final int maxCalculationThreads = 4;

        Cache cache = new Cache(sources);

        var readManager = new ReadManager(sources, cache);
        var results = new Md5Results(sources);
        var calculatorManager = new Md5CalculatorManager(sources, cache, maxCalculationThreads, results);

        new Thread(readManager).start();
        new Thread(calculatorManager).start();
        new Thread(results).start();
*/




        final int maxCalculationThreads = 2;

        var eventManager = new EventManager();

        var printManager = new PrintManager(eventManager);
        var sourcesManager = new SourceManager(sources, eventManager);
        var estimateManager = new EstimateManager(eventManager);
        var readManager = new ReadManager(eventManager);
        var calcManager = new CalculatorManager(maxCalculationThreads, eventManager);
        var resultManager = new ResultManager(eventManager);

        new Thread(printManager).start();
        new Thread(sourcesManager).start();
        new Thread(estimateManager).start();
        new Thread(readManager).start();
        new Thread(calcManager).start();
        new Thread(resultManager).start();
    }




}
