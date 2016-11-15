package it.unipi.di.acube.searchapi.google;

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

import it.unipi.di.acube.searchapi.interfaces.CacheableWebSearchApi;
import it.unipi.di.acube.searchapi.interfaces.WebSearchApi;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;
import it.unipi.di.acube.searchapi.model.WebsearchResponseEntry;

/**
 * Interface to the Google Custom Search Engine (CSE) API.
 * @author Marco Cornolti
 *
 */
public class GoogleSearchApi implements CacheableWebSearchApi {
    public final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String API_PROTOCOL = "https";
    private static final String API_HOST = "www.googleapis.com";
    private static final String API_PATH = "/customsearch/v1";
    private static final String DEFAULT_GEOLOCATION = "en-US";
    private static final String DEFAULT_GOOGLEHOST = "google.com";
    private static final SafeSearchOpt DEFAULT_SAFE_SEARCH = SafeSearchOpt.OFF;

    private static URIBuilder builder = new URIBuilder();

    private String cseId;
    private String apiKey;
    private String geolocation = DEFAULT_GEOLOCATION;
    private String googleHost = DEFAULT_GOOGLEHOST;
    private SafeSearchOpt safeSearch = DEFAULT_SAFE_SEARCH;
    private JSONObject origJson;

    public enum SafeSearchOpt {
        OFF, MEDIUM, HIGH
    }

    /**
     * @param apiKey
     *            the key that will be used to access the Google CSE API. For cache-only usage, you may set this value to null.
     * @param cseId
     *            the custom search engine ID.
     */
    public GoogleSearchApi(String cseId, String apiKey) {
        this.cseId = cseId;
        this.apiKey = apiKey;
    }

    /**
     * @param geolocation
     *            the user geolocation, e.g. "en-US"
     * @return
     */
    public GoogleSearchApi setGeolocation(String geolocation) {
        if (!geolocation.matches("[a-z][a-z]"))
            throw new IllegalArgumentException("Geolocation must be in the form 'en'");
        this.geolocation = geolocation;
        return this;
    }

    public GoogleSearchApi setSafeSearch(SafeSearchOpt safeSearch) {
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
    public boolean recacheNeeded(JSONObject googleCseReply) throws JSONException {
        if (googleCseReply == null)
            return true;
        String type = googleCseReply.getString("kind");
        if (type == null)
            return true;
        return false;
    }

    @Override
    public synchronized WebsearchResponse query(String query) throws Exception {
        URI uri = buildURI(query).addParameter("key", apiKey).addParameter("cx", cseId).build();
        HttpGet get = new HttpGet(uri);
        get.setHeader("Accept", "*/*");
        get.setHeader("Content-Type", "multipart/form-data");

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(get);

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("Got HTTP error {}. Message is: {}", response.getStatusLine().getStatusCode(),
                    IOUtils.toString(response.getEntity().getContent(), "utf-8"));
            throw new RuntimeException("Got response code:" + response.getStatusLine().getStatusCode());
        }
        origJson = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "utf-8"));

        return buildResponseFromJson(origJson);
    }

    public WebsearchResponse buildResponseFromJson(JSONObject googleCseResponse) throws JSONException {
        long totalEstimatedMatches = googleCseResponse.getJSONObject("searchInformation").getLong("totalResults");
        List<WebsearchResponseEntry> webEntries = getWebEntries(googleCseResponse.getJSONArray("items"));

        return new WebsearchResponse(totalEstimatedMatches, webEntries, googleCseResponse);
    }

    private static List<WebsearchResponseEntry> getWebEntries(JSONArray items) throws JSONException {
        Vector<WebsearchResponseEntry> webEntries = new Vector<>();

        for (int i = 0; i < items.length(); i++) {
            JSONObject entry = items.getJSONObject(i);
            String snippet = Jsoup.parse(entry.getString("htmlSnippet").replaceAll("<b>", WebSearchApi.SNIPPET_BOLD_START_STR)
                    .replaceAll("</b>", WebSearchApi.SNIPPET_BOLD_END_STR)).text();
            webEntries.add(new WebsearchResponseEntry(entry.getString("title"), entry.getString("link"), snippet, null));
        }
        return webEntries;
    }

    private URIBuilder buildURI(String query) {
        builder.clearParameters().setScheme(API_PROTOCOL).setHost(API_HOST).setPath(API_PATH).addParameter("q", query)
                .addParameter("gl", geolocation).addParameter("googleHost", googleHost)
                .addParameter("safe", safeSearchToString(safeSearch));
        return builder;
    }

    @Override
    public URI getQueryURI(String query) throws URISyntaxException {
        return buildURI(query).build();
    }

    @Override
    public JSONObject getOriginalJson() {
        return origJson;
    }
}
