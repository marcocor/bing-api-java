package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedWebsearchApi;
import it.unipi.di.acube.searchapi.callers.BingSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

public class QueryBing {

    public static void main(String[] args) throws Exception {
        BingSearchApiCaller bing = new BingSearchApiCaller(args[0]);
        CachedWebsearchApi cached = new CachedWebsearchApi(bing, args[3]);
        WebsearchResponse response = cached.query(args[1], Integer.parseInt(args[2]));

        System.out.printf("Estimated result count: %d\n", response.getTotalResults());

        int i = 0;
        for (WebsearchResponseEntry entry : response.getWebEntries())
            System.out.printf("%d: %s\n%s\n%s\nCrawled on %s\n\n", ++i, entry.getName(), entry.getDisplayUrl(),
                    entry.getSnippet(), entry.getLastCrawled().toString());
        cached.close();
    }
}
