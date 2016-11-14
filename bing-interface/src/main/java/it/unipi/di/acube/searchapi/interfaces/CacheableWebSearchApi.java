package it.unipi.di.acube.searchapi.interfaces;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.searchapi.model.WebsearchResponse;

public interface CacheableWebSearchApi extends WebSearchApi{

    boolean recacheNeeded(JSONObject result) throws JSONException;

    JSONObject getOriginalJson();

    WebsearchResponse buildResponseFromJson(JSONObject result) throws JSONException;

}
