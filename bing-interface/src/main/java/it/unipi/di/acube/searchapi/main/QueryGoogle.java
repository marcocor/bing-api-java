package it.unipi.di.acube.searchapi.main;

import it.unipi.di.acube.searchapi.CachedWebsearchApi;
import it.unipi.di.acube.searchapi.callers.GoogleSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

public class QueryGoogle {

    /**
     * This java main issues a query to the Google CSE API and prints results.
     * 
     * @param args
     *            The command-line arguments in the following order: GOOGLE_CSE_ID, GOOGLE_API_KEY, CACHE_FILE, QUERY, N_RESULTS.
     *            GOOGLE_CSE_ID is the ID of the custom search engine you want to query, GOOGLE_API_KEY is the authentication key
     *            released by Google, CACHE_FILE is the name of the file where results are stored, QUERY is the query to issue,
     *            N_RESULTS is the number of results needed (if this exceeds the number of results returned by one call, multiple
     *            call will be issued).
     * 
     * @throws Exception
     *             if something went wrong.
     */
    public static void main(String[] args) throws Exception {
        GoogleSearchApiCaller google = new GoogleSearchApiCaller(args[0], args[1]);
        CachedWebsearchApi cached = CachedWebsearchApi.builder().api(google).path(args[2]).create();
        WebsearchResponse response = cached.query(args[3], Integer.parseInt(args[4]));

        System.out.printf("Estimated result count: %d\n", response.getTotalResults());

        int i = 0;
        for (WebsearchResponseEntry entry : response.getWebEntries())
            System.out.printf("%d: %s\n%s\n%s\n\n", ++i, entry.getName(), entry.getDisplayUrl(), entry.getSnippet());
        cached.close();
    }
}
