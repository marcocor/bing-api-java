package it.unipi.di.acube.searchapi.model;

import java.util.*;

import org.codehaus.jettison.json.JSONObject;

public class WebsearchResponse {
    long totalEstimatedMatches;
    JSONObject jsonResponse;
    List<WebsearchResponseEntry> webEntries;

    public WebsearchResponse(long totalEstimatedMatches, List<WebsearchResponseEntry> webEntries, JSONObject jsonResponse) {
        this.totalEstimatedMatches = totalEstimatedMatches;
        this.webEntries = webEntries;
        this.jsonResponse = jsonResponse;
    };

    public long getTotalResults() {
        return totalEstimatedMatches;
    }

    public List<WebsearchResponseEntry> getWebEntries() {
        return webEntries;
    }

    public JSONObject getJsonResponse() {
        return jsonResponse;
    }
}
