package md5.stage1sourcesdata;

public class Id {
    private static int nextId = 0;
    public final int id;
    public Id(){ id=nextId++; }
}
