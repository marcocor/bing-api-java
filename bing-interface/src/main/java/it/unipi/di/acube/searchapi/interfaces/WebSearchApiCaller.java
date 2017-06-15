package it.unipi.di.acube.searchapi.interfaces;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.searchapi.model.WebsearchResponse;

/**
 * This interface represents a web-search API that, for each call, returns a number of results starting of an offset.
 * 
 * @author Marco Cornolti
 *
 */
public interface WebSearchApiCaller {

    /**
     * @param response
     *            a response given by this API.
     * @return the number of web results in a single response.
     * @throws JSONException if the response object was unreadable.
     */
    int countResults(JSONObject response) throws JSONException;

    /**
     * @param query
     *            a query
     * @param resultsSoFar
     *            the number of results that have been already returned for this query (this determines the search offset).
     * @return the JSON object returned by the API.
     * @throws Exception
     */
    JSONObject query(String query, int resultsSoFar) throws Exception;

    /**
     * @param jsonResponses
     *            a list of subsequent responses provided by the search engine API for a single query.
     * @return whether or not the responses are badly formed and requests need to be done again.
     * @throws JSONException if one of the response objects was unreadable.
     */
    boolean recacheNeeded(List<JSONObject> jsonResponses) throws JSONException;

    /**
     * @param uris
     *            the set of URIS that have been called for a query
     * @param jsonResponses
     *            a set of responses for a single query, ordered by search offset.
     * @param neededResults
     *            how many results are requested.
     * @return an aggregated result for a query.
     * @throws JSONException if one of the response objects was unreadable.
     */
    WebsearchResponse buildResponseFromJson(List<URI> uris, List<JSONObject> jsonResponses, int neededResults)
            throws JSONException;

    /**
     * @param query
     *            a query.
     * @param resultsSoFar
     *            the number of results that have been already returned for this query (this determines the search offset).
     * @return a URI univocally representing the call to the API for this query.
     * @throws URISyntaxException if it was not possible to build the URI.
     */
    URI getQueryURI(String query, int resultsSoFar) throws URISyntaxException;

    /**
     * @param jsonResponses
     *            a set of responses for a single query, ordered by search offset.
     * @param neededResults
     *            how many results are requested.
     * @return true iff the query is complete, i.e. does not need more querying.
     * @throws JSONException if one of the response objects was unreadable.
     */
    boolean queryComplete(List<JSONObject> jsonResponses, int neededResults) throws JSONException;

}
