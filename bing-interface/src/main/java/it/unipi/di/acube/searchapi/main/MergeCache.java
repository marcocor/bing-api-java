package it.unipi.di.acube.searchapi.main;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;

import it.unipi.di.acube.searchapi.CachedSearchApi;

public class MergeCache {
    public static void main(String[] args) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[0]));
        HashMap<String, byte[]> url2jsonCache2 = (HashMap<String, byte[]>) ois.readObject();
        ois.close();

        CachedSearchApi c = new CachedSearchApi(null, args[1]);

        c.mergeCache(url2jsonCache2);

        c.flush();
    }

}
