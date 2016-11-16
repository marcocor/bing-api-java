package it.unipi.di.acube.searchapi.model;

import java.net.URI;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;

public class WebsearchResponse {
    private long totalEstimatedMatches;
    private List<WebsearchResponseEntry> webEntries;
    private List<URI> calledUris;
    private List<JSONObject> responses;

    public WebsearchResponse(long totalEstimatedMatches, List<WebsearchResponseEntry> webEntries, List<URI> calledURIs,
            List<JSONObject> responses) {
        this.totalEstimatedMatches = totalEstimatedMatches;
        this.webEntries = webEntries;
        this.responses = responses;
        this.calledUris = calledURIs;
    }

    public long getTotalResults() {
        return totalEstimatedMatches;
    }

    public List<WebsearchResponseEntry> getWebEntries() {
        return webEntries;
    }

    public List<JSONObject> getJsonResponses() {
        return responses;
    }

    public List<URI> getCalledURIs() {
        return calledUris;
    }
}
