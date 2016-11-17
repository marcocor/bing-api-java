package it.unipi.di.acube.searchapi.main;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import it.unipi.di.acube.searchapi.CachedWebsearchApi;

public class FixCache {

    public static void main(String[] args) throws Exception {
        DB db = DBMaker.fileDB(args[0]).fileMmapEnable().closeOnJvmShutdownWeakReference().checksumHeaderBypass().make();
        db.close();

        CachedWebsearchApi cached = new CachedWebsearchApi(null, args[0]);
        System.out.printf("Total cached URIs: %d", cached.getCachedRequests());
        cached.close();
    }
}
