package it.unipi.di.acube.searchapi.callers;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.searchapi.WebsearchApi;
import it.unipi.di.acube.searchapi.interfaces.WebSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

/**
 * Interface to the Google Custom Search Engine (CSE) API.
 * 
 * @author Marco Cornolti
 *
 */
public class GoogleSearchApiCaller implements WebSearchApiCaller {
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String API_PROTOCOL = "https";
    private static final String API_HOST = "www.googleapis.com";
    private static final String API_PATH = "/customsearch/v1";
    private static final int MAX_CSE_RESULTS = 10;
    private static final String DEFAULT_GEOLOCATION = "us";
    private static final String DEFAULT_GOOGLEHOST = "google.com";
    private static final SafeSearchOpt DEFAULT_SAFE_SEARCH = SafeSearchOpt.OFF;
    private static URIBuilder builder = new URIBuilder();

    private String cseId;
    private String apiKey;
    private String geolocation = DEFAULT_GEOLOCATION;
    private String googleHost = DEFAULT_GOOGLEHOST;
    private SafeSearchOpt safeSearch = DEFAULT_SAFE_SEARCH;

    public enum SafeSearchOpt {
        OFF, MEDIUM, HIGH
    }

    /**
     * @param apiKey
     *            the key that will be used to access the Google CSE API. For cache-only usage, you may set this value to null.
     * @param cseId
     *            the custom search engine ID.
     */
    public GoogleSearchApiCaller(String cseId, String apiKey) {
        this.cseId = cseId;
        this.apiKey = apiKey;
    }

    /**
     * @param geolocation
     *            the user geolocation, e.g. "en-US" (see Google Custom Search Engine reference).
     * @return this.
     */
    public GoogleSearchApiCaller setGeolocation(String geolocation) {
        if (!geolocation.matches("[a-z][a-z]"))
            throw new IllegalArgumentException("Geolocation must be in the form 'en'");
        this.geolocation = geolocation;
        return this;
    }

    /**
     * @param safeSearch
     *            the safeSearch parameter (see Google Custom Search Engine reference).
     * @return this.
     */
    public GoogleSearchApiCaller setSafeSearch(SafeSearchOpt safeSearch) {
        this.safeSearch = safeSearch;
        return this;
    }

    private String safeSearchToString(SafeSearchOpt opt) {
        switch (opt) {
        case OFF:
            return "off";
        case MEDIUM:
            return "medium";
        default:
            return "high";
        }
    }

    @Override
    public synchronized JSONObject query(String query, int resultsSoFar) throws Exception {
        URI uri = buildURI(query, resultsSoFar + 1).addParameter("key", apiKey).addParameter("cx", cseId).build();
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", "*/*");
        get.setHeader("Content-Type", "multipart/form-data");

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

    private URIBuilder buildURI(String query, int queryStart) {
        builder.clearParameters().setScheme(API_PROTOCOL).setHost(API_HOST).setPath(API_PATH).addParameter("q", query)
                .addParameter("gl", geolocation).addParameter("googleHost", googleHost)
                .addParameter("safe", safeSearchToString(safeSearch)).addParameter("start", Integer.toString(queryStart))
                .addParameter("num", Integer.toString(MAX_CSE_RESULTS));
        return builder;
    }

    @Override
    public URI getQueryURI(String query, int resultsSoFar) throws URISyntaxException {
        return buildURI(query, resultsSoFar + 1).build();
    }

    @Override
    public int countResults(JSONObject cseResponse) throws JSONException {
        if (!cseResponse.has("items"))
            return 0;
        return cseResponse.getJSONArray("items").length();
    }

    @Override
    public boolean recacheNeeded(List<JSONObject> jsonResponses) throws JSONException {
        for (JSONObject jsonResponse : jsonResponses) {
            if (jsonResponse == null)
                return true;
            String type = jsonResponse.getString("kind");
            if (type == null)
                return true;
        }
        return false;
    }

    @Override
    public WebsearchResponse buildResponseFromJson(List<URI> calledUris, List<JSONObject> jsonResponses, int neededResults)
            throws JSONException {
        long totalEstimatedMatches = jsonResponses.get(0).getJSONObject("searchInformation").getLong("totalResults");

        List<WebsearchResponseEntry> webEntries = new Vector<>();
        for (JSONObject bingResponse : jsonResponses)
            if (bingResponse.has("items")) {
                List<WebsearchResponseEntry> webEntriesI = getWebEntries(bingResponse.getJSONArray("items"));
                for (WebsearchResponseEntry entry : webEntriesI) {
                    if (neededResults == 0)
                        break;
                    webEntries.add(entry);
                    neededResults--;
                }
                if (neededResults == 0)
                    break;
            }

        return new WebsearchResponse(totalEstimatedMatches, webEntries, calledUris, jsonResponses);
    }

    private static List<WebsearchResponseEntry> getWebEntries(JSONArray items) throws JSONException {
        Vector<WebsearchResponseEntry> webEntries = new Vector<>();

        for (int i = 0; i < items.length(); i++) {
            JSONObject entry = items.getJSONObject(i);
            String snippet = Jsoup.parse(entry.getString("htmlSnippet").replaceAll("<b>", WebsearchApi.SNIPPET_BOLD_START_STR)
                    .replaceAll("</b>", WebsearchApi.SNIPPET_BOLD_END_STR)).text();
            webEntries.add(new WebsearchResponseEntry(entry.getString("title"), entry.getString("link"), snippet, null));
        }
        return webEntries;
    }

    @Override
    public boolean queryComplete(List<JSONObject> jsonResponses, int neededResults) throws JSONException {
        int count = 0;
        for (JSONObject jsonResponse : jsonResponses) {
            int res = countResults(jsonResponse);
            if (res < MAX_CSE_RESULTS)
                return true;
            count += res;
            if (count >= neededResults)
                return true;
        }
        return false;
    }
}
