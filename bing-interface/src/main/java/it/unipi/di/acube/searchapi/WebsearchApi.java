package it.unipi.di.acube.searchapi;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;
import java.util.Vector;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.searchapi.interfaces.WebSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;

public class WebsearchApi {
    public static final char SNIPPET_BOLD_START = 0xe000;
    public static final char SNIPPET_BOLD_END = 0xe001;
    public static final String SNIPPET_BOLD_START_STR = new String(Character.toString(SNIPPET_BOLD_START));
    public static final String SNIPPET_BOLD_END_STR = new String(Character.toString(SNIPPET_BOLD_END));

    private final int MAX_RETRY = 3;
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    WebSearchApiCaller api;

    public WebsearchApi(WebSearchApiCaller api) {
        this.api = api;
    }

    /**
     * Get the response for a query. If the response is not cached, issue it.
     * 
     * @param query
     *            the query.
     * @param neededResults
     *            how many results are needed (higher numbers may result in higher number of queries).
     * @return the response to the query
     * @throws Exception
     *             is the call to the API failed.
     */
    public WebsearchResponse query(String query, int neededResults) throws Exception {
        return query(query, neededResults, MAX_RETRY);
    }

    private synchronized WebsearchResponse query(String query, int neededResults, int retryLeft) throws Exception {
        int resultsSoFar = 0;
        List<JSONObject> jsonResponses = new Vector<>();
        List<URI> uris = new Vector<>();
        do {
            uris.add(api.getQueryURI(query, resultsSoFar));
            JSONObject response = api.query(query, resultsSoFar);
            jsonResponses.add(response);
            resultsSoFar += api.countResults(response);
        } while (!api.queryComplete(jsonResponses, neededResults));

        if (api.recacheNeeded(jsonResponses) && retryLeft > 0) {
            LOG.warn("Bad responses, calling API again.");
            Thread.sleep(1000);
            return query(query, retryLeft - 1);
        }

        return api.buildResponseFromJson(uris, jsonResponses, neededResults);
    }

}
