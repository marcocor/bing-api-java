package it.unipi.di.acube.searchapi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.codehaus.jettison.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.HTreeMap.KeySet;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unipi.di.acube.searchapi.interfaces.CacheableWebSearchApi;
import it.unipi.di.acube.searchapi.interfaces.WebSearchApi;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;

public class CachedSearchApi implements WebSearchApi {
    public final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int flushCounter = 0;
    private final int MAX_RETRY = 3;
    private DB db;
    private HTreeMap<String, byte[]> queryResponses;
    private CacheableWebSearchApi api;

    public CachedSearchApi(CacheableWebSearchApi api, String cachePath)
            throws FileNotFoundException, ClassNotFoundException, IOException {
        this.api = api;
        this.db = DBMaker.fileDB(cachePath).fileMmapEnable().closeOnJvmShutdown().make();
        this.queryResponses = db.hashMap("queries", Serializer.STRING, Serializer.BYTE_ARRAY).createOrOpen();
    }

    /**
     * Get the response for a query. If the response is not cached, issue it.
     * 
     * @param query
     *            the query.
     * @return the response to the query
     * @throws Exception
     *             is the call to the API failed.
     */
    public synchronized WebsearchResponse query(String query) throws Exception {
        return query(query, MAX_RETRY);
    }

    private synchronized WebsearchResponse query(String query, int retryLeft) throws Exception {
        boolean forceCacheOverride = retryLeft < MAX_RETRY;
        if (forceCacheOverride)
            Thread.sleep(1000);

        URI uri = api.getQueryURI(query);

        JSONObject result = queryResponses.containsKey(uri.toString())
                ? new JSONObject(StringCompress.decompress(queryResponses.get(uri.toString()))) : null;

        boolean cached = !forceCacheOverride && result != null;
        LOG.info("{}{} {}", forceCacheOverride ? "<forceCacheOverride>" : "", cached ? "<cached>" : "Querying", uri);
        if (!cached) {
            api.query(query);
            result = api.getOriginalJson();
            queryResponses.put(uri.toString(), StringCompress.compress(result.toString()));
        }

        if (api.recacheNeeded(result) && retryLeft > 0)
            return query(query, retryLeft - 1);

        return api.buildResponseFromJson(result);
    }

    /**
     * Add all records contained in the cache passed by argument to the static cache, overwriting in case of conflicting keys.
     * 
     * @param newCache
     *            the cache whose records are added.
     */
    public void mergeCache(HashMap<String, byte[]> newCache) {
        for (String key : newCache.keySet()) {
            queryResponses.put(key, newCache.get(key));
            flushCounter++;
        }
    }

    public synchronized void flush() throws FileNotFoundException, IOException {
        if (flushCounter > 0) {
            LOG.info("Flushing Websearch cache...");
            db.commit();
            LOG.info("Flushing Websearch cache done.");
        }
    }

    @Override
    public URI getQueryURI(String query) throws URISyntaxException {
        return api.getQueryURI(query);
    }

    public long getCachedRequests() {
        return queryResponses.sizeLong();
    }

    public KeySet<String> getCachedURIs() {
        return queryResponses.getKeys();
    }
}