package it.unipi.di.acube.searchapi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.codehaus.jettison.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.HTreeMap.KeySet;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.searchapi.interfaces.WebSearchApiCaller;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;

public class CachedWebsearchApi extends WebsearchApi {
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private DB db;
    private HTreeMap<String, byte[]> queryResponses;

    public CachedWebsearchApi(WebSearchApiCaller api, String cachePath)
            throws FileNotFoundException, ClassNotFoundException, IOException {
        super(api);
        this.db = DBMaker.fileDB(cachePath).fileMmapEnable().closeOnJvmShutdownWeakReference().make();
        this.queryResponses = db.hashMap("queries", Serializer.STRING, Serializer.BYTE_ARRAY).createOrOpen();
    }

    @Override
    public synchronized WebsearchResponse query(String query, int neededResults) throws Exception {
        int resultsSoFar = 0;
        boolean cached = true;
        List<URI> calledUris = new Vector<>();
        List<JSONObject> jsonResponses = new Vector<>();
        do {
            URI uri = api.getQueryURI(query, resultsSoFar);
            calledUris.add(uri);
            cached = queryResponses.containsKey(uri.toString());
            if (cached) {
                JSONObject cachedResponse = new JSONObject(StringCompress.decompress(queryResponses.get(uri.toString())));
                jsonResponses.add(cachedResponse);
                resultsSoFar += api.countResults(cachedResponse);
            }
        } while (cached && !api.queryComplete(jsonResponses, neededResults));

        if (cached) {
            for (URI uri : calledUris)
                LOG.info("<cached> {}", uri);
            return api.buildResponseFromJson(calledUris, jsonResponses, neededResults);
        } else {
            WebsearchResponse result = super.query(query, neededResults);
            for (int i = 0; i < result.getCalledURIs().size(); i++) {
                queryResponses.put(result.getCalledURIs().get(i).toString(),
                        StringCompress.compress(result.getJsonResponses().get(i).toString()));
            }
            db.commit();
            return result;
        }
    }

    /**
     * Add all records contained in the cache passed by argument to the static cache, overwriting in case of conflicting keys.
     * 
     * @param newCache
     *            the cache whose records are added.
     */
    public synchronized void mergeCache(HashMap<String, byte[]> newCache) {
        for (String key : newCache.keySet())
            queryResponses.put(key, newCache.get(key));
        db.commit();
    }

    /**
     * @return the number of requests contained in the cache.
     */
    public long getCachedRequests() {
        return queryResponses.sizeLong();
    }

    /**
     * Close the database (further read/write will throw an exception).
     */
    public synchronized void close() {
        db.close();
    }

    /**
     * @return the set of cached URIs.
     */
    public KeySet<String> cachedUris() {
        return queryResponses.getKeys();
    }
}