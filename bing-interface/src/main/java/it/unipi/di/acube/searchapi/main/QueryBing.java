package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedSearchApi;
import it.unipi.di.acube.searchapi.bing.BingSearchApi;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

public class QueryBing {

    public static void main(String[] args) throws Exception {
        BingSearchApi bing = new BingSearchApi(args[0]);
        CachedSearchApi cached = new CachedSearchApi(bing, args[2]);
        WebsearchResponse response = cached.query(args[1]);

        System.out.printf("Estimated result count: %d\n", response.getTotalResults());

        int i = 0;
        for (WebsearchResponseEntry entry : response.getWebEntries())
            System.out.printf("%d: %s\n%s\n%s\nCrawled on %s\n\n", ++i, entry.getName(), entry.getDisplayUrl(),
                    entry.getSnippet(), entry.getLastCrawled().toString());
        cached.flush();
    }

}
