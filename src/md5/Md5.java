package md5;

import java.util.*;

public class Md5 {

    public static void main(String[] args) throws InterruptedException {

        /*final Set<SourceInfo> sources = Set.of(
            new SourceInfo("D:\\ТОРРЕНТЫ", "D:"),
            new SourceInfo("E:\\[test]", "E: 2.5\"")
        );*/
        final Set<SourceInfo> sources = Set.of(
            new SourceInfo("I:\\test\\1", "I"),
            new SourceInfo("I:\\test\\2", "I"),
            new SourceInfo("F:\\test\\3", "F")
        );
        final int nSimultaneousThreads = 4;

        Cache cache = new Cache(sources);
        new Thread(new FileReadManager(sources, cache)).start();
        Md5Results results = new Md5Results(sources);
        new Thread(new Md5CalculatorManager(sources, cache, nSimultaneousThreads, results)).start();

    }




}
