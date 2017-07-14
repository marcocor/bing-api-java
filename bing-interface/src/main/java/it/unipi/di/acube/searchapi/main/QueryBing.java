package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedWebsearchApi;
import it.unipi.di.acube.searchapi.callers.BingSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

public class QueryBing {

    /**
     * This java main issues a query to the Bing API and prints results.
     * 
     * @param args
     *            The command-line arguments in the following order: BING_KEY, QUERY, N_RESULTS, CACHE_FILE. BING_KEY is the
     *            authentication key to the Bing Search API; QUERY is the query to issue; N_RESULTS is the number of results
     *            needed (if this exceeds the number of results returned by one call, multiple call will be issued), CACHE_FILE is
     *            the name of the file where results are stored.
     * 
     * @throws Exception
     *             if something went wrong.
     */
    public static void main(String[] args) throws Exception {
        BingSearchApiCaller bing = new BingSearchApiCaller(args[0]);
        CachedWebsearchApi cached = CachedWebsearchApi.builder().api(bing).path(args[3]).create();
        WebsearchResponse response = cached.query(args[1], Integer.parseInt(args[2]));

        System.out.printf("Estimated result count: %d\n", response.getTotalResults());

        int i = 0;
        for (WebsearchResponseEntry entry : response.getWebEntries())
            System.out.printf("%d: %s\n%s\n%s\nCrawled on %s\n\n", ++i, entry.getName(), entry.getDisplayUrl(),
                    entry.getSnippet(), entry.getLastCrawled().toString());
        cached.close();
    }
}
