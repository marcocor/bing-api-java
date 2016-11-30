package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedWebsearchApi;

public class CacheStats {

    public static void main(String[] args) throws Exception {
        CachedWebsearchApi cached = CachedWebsearchApi.builder().path(args[0]).create();
        System.out.println("List of cached URIs:");
        for (String uri : cached.cachedUris())
            System.out.printf("%s\n", uri);
        System.out.printf("Total cached URIs: %d", cached.getCachedRequests());
        cached.close();
    }
}
