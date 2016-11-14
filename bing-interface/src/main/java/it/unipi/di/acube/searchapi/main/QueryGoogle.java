package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedSearchApi;
import it.unipi.di.acube.searchapi.google.GoogleSearchApi;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

public class QueryGoogle {

    public static void main(String[] args) throws Exception {
        GoogleSearchApi google = new GoogleSearchApi(args[0], args[1]);
        CachedSearchApi cached = new CachedSearchApi(google, args[2]);
        WebsearchResponse response = cached.query(args[3]);

        System.out.printf("Estimated result count: %d\n", response.getTotalResults());

        int i = 0;
        for (WebsearchResponseEntry entry : response.getWebEntries())
            System.out.printf("%d: %s\n%s\n%s\n\n", ++i, entry.getName(), entry.getDisplayUrl(), entry.getSnippet());
        cached.flush();
    }

}
