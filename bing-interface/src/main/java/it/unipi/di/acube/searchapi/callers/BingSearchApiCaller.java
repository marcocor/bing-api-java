package it.unipi.di.acube.searchapi.callers;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.searchapi.WebsearchApi;
import it.unipi.di.acube.searchapi.interfaces.WebSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

/**
 * Interface to the Bing Search API.
 * 
 * @author Marco Cornolti
 *
 */
public class BingSearchApiCaller implements WebSearchApiCaller {
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String API_PROTOCOL = "https";
    private static final String API_HOST = "api.cognitive.microsoft.com";
    private static final String API_PATH = "/bing/v5.0/search";
    private static final String DEFAULT_MARKET = "en-US";
    private static final String DEFAULT_RESPONSE_FILTER = "RelatedSearches,SpellSuggestions,Webpages";
    private static final SafeSearchOpt DEFAULT_SAFE_SEARCH = SafeSearchOpt.OFF;
    private static final int MAX_RESULTS_PER_QUERY = 50;
    private static final boolean TEXT_DECORATIONS = true;
    private static URIBuilder builder = new URIBuilder();

    private String bingKey;
    private String market = DEFAULT_MARKET;
    private String responseFilter = DEFAULT_RESPONSE_FILTER;
    private SafeSearchOpt safeSearch = DEFAULT_SAFE_SEARCH;

    public enum SafeSearchOpt {
        OFF, MODERATE, STRICT
    }

    /**
     * @param bingKey
     *            the key that will be used to access the Bing API.
     */
    public BingSearchApiCaller(String bingKey) {
        this.bingKey = bingKey;
    }

    /**
     * @param market
     *            the market, e.g. "en-US" (see Bing API reference).
     * @return this.
     */
    public BingSearchApiCaller setMarket(String market) {
        if (!market.matches("[a-z][a-z]-[A-Z][A-Z]"))
            throw new IllegalArgumentException("Market must be in the form en-US");
        this.market = market;
        return this;
    }

    /**
     * @param responseFilter
     *            the response filter (see Bing API reference).
     * @return this.
     */
    public BingSearchApiCaller setResponseFilter(String responseFilter) {
        this.responseFilter = responseFilter;
        return this;
    }

    /**
     * @param safeSearch
     *            the safeSearch parameter (see Bing API reference).
     * @return this.
     */
    public BingSearchApiCaller setSafeSearch(SafeSearchOpt safeSearch) {
        this.safeSearch = safeSearch;
        return this;
    }

    private static String safeSearchToString(SafeSearchOpt opt) {
        switch (opt) {
        case OFF:
            return "Off";
        case MODERATE:
            return "Moderate";
        default:
            return "Strict";
        }
    }

    @Override
    public JSONObject query(String query, int resultsSoFar) throws Exception {
        URI uri = getQueryURI(query, resultsSoFar);
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", "*/*");
        get.setHeader("Content-Type", "multipart/form-data");
        get.setHeader("Ocp-Apim-Subscription-Key", bingKey);

        HttpClient httpClient = HttpClientBuilder.create().build();
        LOG.info("<querying> {}", uri.toString());
        HttpResponse response = httpClient.execute(get);

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("Got HTTP error {}. Message is: {}", response.getStatusLine().getStatusCode(),
                    IOUtils.toString(response.getEntity().getContent(), "utf-8"));
            throw new RuntimeException("Got response code:" + response.getStatusLine().getStatusCode());
        }
        return new JSONObject(IOUtils.toString(response.getEntity().getContent(), "utf-8"));
    }

    @Override
    public URI getQueryURI(String query, int resultsSoFar) throws URISyntaxException {
        URI uri = builder.clearParameters().setScheme(API_PROTOCOL).setHost(API_HOST).setPath(API_PATH).addParameter("q", query)
                .addParameter("count", Integer.toString(MAX_RESULTS_PER_QUERY)).addParameter("mkt", market)
                .addParameter("responseFilter", responseFilter).addParameter("safeSearch", safeSearchToString(safeSearch))
                .addParameter("textDecorations", Boolean.toString(TEXT_DECORATIONS))
                .addParameter("offset", Integer.toString(resultsSoFar)).build();
        return uri;
    }

    @Override
    public int countResults(JSONObject bingResponse) throws JSONException {
        if (!bingResponse.has("webPages"))
            return 0;
        return bingResponse.getJSONObject("webPages").getJSONArray("value").length();
    }

    @Override
    public boolean recacheNeeded(List<JSONObject> bingResponses) throws JSONException {
        for (JSONObject bingResponse : bingResponses) {
            if (bingResponse == null)
                return true;
            String type = bingResponse.getString("_type");
            if (type == null)
                return true;
        }
        return false;
    }

    @Override
    public WebsearchResponse buildResponseFromJson(List<URI> calledUris, List<JSONObject> bingResponses, int neededResults)
            throws JSONException {
        long totalEstimatedMatches = bingResponses.get(0).has("webPages")
                ? bingResponses.get(0).getJSONObject("webPages").getLong("totalEstimatedMatches") : 0;

        List<WebsearchResponseEntry> webEntries = new Vector<>();
        for (JSONObject bingResponse : bingResponses)
            if (bingResponse.has("webPages")) {
                List<WebsearchResponseEntry> webEntriesI = getWebEntries(
                        bingResponse.getJSONObject("webPages").getJSONArray("value"));

                for (WebsearchResponseEntry entry : webEntriesI) {
                    if (neededResults == 0)
                        break;
                    webEntries.add(entry);
                    neededResults--;
                }
                if (neededResults == 0)
                    break;
            }

        return new WebsearchResponse(totalEstimatedMatches, webEntries, calledUris, bingResponses);
    }

    private static List<WebsearchResponseEntry> getWebEntries(JSONArray value) throws JSONException {
        Vector<WebsearchResponseEntry> webEntries = new Vector<>();

        for (int i = 0; i < value.length(); i++) {
            JSONObject entry = value.getJSONObject(i);
            Date lastCrawled = entry.has("dateLastCrawled")
                    ? DatatypeConverter.parseDateTime(entry.getString("dateLastCrawled")).getTime() : null;
            webEntries.add(new WebsearchResponseEntry(entry.getString("name"), entry.getString("displayUrl")
                    .replaceAll(WebsearchApi.SNIPPET_BOLD_END_STR, "").replaceAll(WebsearchApi.SNIPPET_BOLD_START_STR, ""),
                    entry.getString("snippet"), lastCrawled));
        }
        return webEntries;
    }

    @Override
    public boolean queryComplete(List<JSONObject> jsonResponses, int neededResults) throws JSONException {
        int count = 0;
        for (JSONObject jsonResponse : jsonResponses) {
            count += countResults(jsonResponse);
            if (count >= neededResults)
                return true;
            if (countResults(jsonResponse) == 0)
                return true;
            if (jsonResponse.getJSONObject("rankingResponse").getJSONObject("mainline").getJSONArray("items")
                    .length() < MAX_RESULTS_PER_QUERY)
                return true;
        }
        return false;
    }
}
