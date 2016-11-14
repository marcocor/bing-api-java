package it.unipi.di.acube.searchapi.bing;

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

import it.unipi.di.acube.searchapi.interfaces.CacheableWebSearchApi;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

/**
 * Interface to the Bing Search API.
 * @author Marco Cornolti
 *
 */
public class BingSearchApi implements CacheableWebSearchApi {
    private static final String API_PROTOCOL = "https";
    private static final String API_HOST = "api.cognitive.microsoft.com";
    private static final String API_PATH = "/bing/v5.0/search";
    private static final String DEFAULT_MARKET = "en-US";
    private static final String DEFAULT_RESPONSE_FILTER = "RelatedSearches,SpellSuggestions,Webpages";
    private static final SafeSearchOpt DEFAULT_SAFE_SEARCH = SafeSearchOpt.OFF;
    private static final int MAX_RESULTS = 50;
    private static final boolean TEXT_DECORATIONS = true;
    private static URIBuilder builder = new URIBuilder();

    private String bingKey;
    private String market = DEFAULT_MARKET;
    private String responseFilter = DEFAULT_RESPONSE_FILTER;
    private SafeSearchOpt safeSearch = DEFAULT_SAFE_SEARCH;
    private JSONObject origJson;

    public enum SafeSearchOpt {
        OFF, MODERATE, STRICT
    }

    /**
     * @param bingKey
     *            the key that will be used to access the Bing API. For cache-only usage, you may set this value tu null.
     */
    public BingSearchApi(String bingKey) {
        this.bingKey = bingKey;
    }

    /**
     * @param market
     *            the market, e.g. "en-US"
     * @return
     */
    public BingSearchApi setMarket(String market) {
        if (!market.matches("[a-z][a-z]-[A-Z][A-Z]"))
            throw new IllegalArgumentException("Market must be in the form en-US");
        this.market = market;
        return this;
    }

    public BingSearchApi setResponseFilter(String responseFilter) {
        this.responseFilter = responseFilter;
        return this;
    }

    public BingSearchApi setSafeSearch(SafeSearchOpt safeSearch) {
        this.safeSearch = safeSearch;
        return this;
    }

    private String safeSearchToString(SafeSearchOpt opt) {
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
    public boolean recacheNeeded(JSONObject bingReply) throws JSONException {
        if (bingReply == null)
            return true;
        String type = bingReply.getString("_type");
        if (type == null)
            return true;
        return false;
    }

    @Override
    public synchronized WebsearchResponse query(String query) throws Exception {
        URI uri = getQueryURI(query);
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", "*/*");
        get.setHeader("Content-Type", "multipart/form-data");
        get.setHeader("Ocp-Apim-Subscription-Key", bingKey);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(get);

        if (response.getStatusLine().getStatusCode() != 200) {
            System.err.printf("Got HTTP error %d. Message is: %s%n", response.getStatusLine().getStatusCode(),
                    IOUtils.toString(response.getEntity().getContent(), "utf-8"));
            throw new RuntimeException("Got response code:" + response.getStatusLine().getStatusCode());
        }
        origJson = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "utf-8"));

        return buildResponseFromJson(origJson);
    }

    public WebsearchResponse buildResponseFromJson(JSONObject bingResponse) throws JSONException {
        if (!bingResponse.has("webPages"))
            return new WebsearchResponse(0, new Vector<WebsearchResponseEntry>(), bingResponse);

        long totalEstimatedMatches = bingResponse.getJSONObject("webPages").getLong("totalEstimatedMatches");
        List<WebsearchResponseEntry> webEntries = getWebEntries(bingResponse.getJSONObject("webPages").getJSONArray("value"));

        return new WebsearchResponse(totalEstimatedMatches, webEntries, bingResponse);
    }

    private static List<WebsearchResponseEntry> getWebEntries(JSONArray value) throws JSONException {
        Vector<WebsearchResponseEntry> webEntries = new Vector<>();

        for (int i = 0; i < value.length(); i++) {
            JSONObject entry = value.getJSONObject(i);
            Date lastCrawled = entry.has("dateLastCrawled")
                    ? DatatypeConverter.parseDateTime(entry.getString("dateLastCrawled")).getTime() : null;
            webEntries.add(new WebsearchResponseEntry(entry.getString("name"), entry.getString("displayUrl"),
                    entry.getString("snippet"), lastCrawled));
        }
        return webEntries;
    }

    @Override
    public URI getQueryURI(String query) throws URISyntaxException {
        URI uri = builder.clearParameters().setScheme(API_PROTOCOL).setHost(API_HOST).setPath(API_PATH).addParameter("q", query)
                .addParameter("count", Integer.toString(MAX_RESULTS)).addParameter("mkt", market)
                .addParameter("responseFilter", responseFilter).addParameter("safeSearch", safeSearchToString(safeSearch))
                .addParameter("textDecorations", Boolean.toString(TEXT_DECORATIONS)).build();
        return uri;
    }

    @Override
    public JSONObject getOriginalJson() {
        return origJson;
    }
}
