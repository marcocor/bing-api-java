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
    private String cachePath;

    /**
     * Builder for CachedWebsearchApi.
     *
     */
    public static class CachedWebsearchApiBuilder {
        private WebSearchApiCaller api;
        private String cachePath;
        private CachedWebsearchApi cachedApi;

        /**
         * @param api
         *            a Websearch API caller
         * @return this builder.
         */
        public CachedWebsearchApiBuilder api(WebSearchApiCaller api) {
            this.api = api;
            return this;
        }

        /**
         * @param path
         *            the database storage path.
         * @return this builder.
         */
        public CachedWebsearchApiBuilder path(String path) {
            this.cachePath = path;
            return this;
        }

        /**
         * @param cachedApi
         *            the other API with an open database to reuse. If the path is specified, the DB of this api must have the
         *            same path. Ignored if null.
         * @return this builder.
         */
        public CachedWebsearchApiBuilder dbFrom(CachedWebsearchApi cachedApi) {
            this.cachedApi = cachedApi;
            return this;
        }

        public CachedWebsearchApi create() throws FileNotFoundException, ClassNotFoundException, IOException {
            if (cachedApi == null && cachePath == null)
                throw new IllegalArgumentException("You need to either specify a storage path or give a cached API to reuse.");
            if (cachedApi != null && cachePath != null && !cachePath.equals(cachedApi.cachePath))
                throw new IllegalArgumentException(String.format(
                        "Trying to reuse Websearch cache but different path provided: %s vs %s", cachePath, cachedApi.cachePath));
            if (cachePath == null)
                cachePath = cachedApi.cachePath;
            DB db;
            if (cachedApi != null) {
                LOG.debug("Reusing already open Webcache database.");
                db = cachedApi.db;
            } else {
                db = DBMaker.fileDB(cachePath).fileMmapEnable().closeOnJvmShutdownWeakReference().make();
            }
            return new CachedWebsearchApi(api, db, cachePath,
                    db.hashMap("queries", Serializer.STRING, Serializer.BYTE_ARRAY).createOrOpen());
        }
    }

    private CachedWebsearchApi(WebSearchApiCaller api, DB db, String cachePath, HTreeMap<String, byte[]> queryResponses)
            throws FileNotFoundException, ClassNotFoundException, IOException {
        super(api);
        this.db = db;
        this.queryResponses = queryResponses;
        this.cachePath = cachePath;
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

    public static CachedWebsearchApiBuilder builder() {
        return new CachedWebsearchApiBuilder();
    }
}