package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedWebsearchApi;
import it.unipi.di.acube.searchapi.callers.GoogleSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

public class QueryGoogle {

    public static void main(String[] args) throws Exception {
        GoogleSearchApiCaller google = new GoogleSearchApiCaller(args[0], args[1]);
        CachedWebsearchApi cached = new CachedWebsearchApi(google, args[2]);
        WebsearchResponse response = cached.query(args[3], Integer.parseInt(args[4]));

        System.out.printf("Estimated result count: %d\n", response.getTotalResults());

        int i = 0;
        for (WebsearchResponseEntry entry : response.getWebEntries())
            System.out.printf("%d: %s\n%s\n%s\n\n", ++i, entry.getName(), entry.getDisplayUrl(), entry.getSnippet());
        cached.close();
    }
}
