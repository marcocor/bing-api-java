package it.unipi.di.acube.searchapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.codehaus.jettison.json.JSONObject;

import it.unipi.di.acube.searchapi.interfaces.CacheableWebSearchApi;
import it.unipi.di.acube.searchapi.interfaces.WebSearchApi;
import it.unipi.di.acube.searchapi.model.WebsearchResponse;

public class CachedSearchApi implements WebSearchApi{
    private int flushCounter = 0;
    private final int MAX_RETRY = 3;
    private final int FLUSH_EVERY = 50;
    private HashMap<String, byte[]> url2jsonCache = new HashMap<>();
    private String resultsCacheFilename;
    private CacheableWebSearchApi api;

    public CachedSearchApi(CacheableWebSearchApi api, String cacheFilename) throws FileNotFoundException, ClassNotFoundException, IOException {
        this.api = api;
        this.setCache(cacheFilename);
    }

    private synchronized void increaseFlushCounter() throws FileNotFoundException, IOException {
        flushCounter++;
        if (flushCounter % FLUSH_EVERY == 0)
            flush();
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

        JSONObject result = null;
        byte[] compressed = url2jsonCache.get(uri.toString());
        if (compressed != null)
            result = new JSONObject(StringCompress.decompress(compressed));

        boolean cached = !forceCacheOverride && result != null;
        System.out.printf("%s%s %s%n", forceCacheOverride ? "<forceCacheOverride>" : "", cached ? "<cached>" : "Querying", uri);
        if (!cached) {
            api.query(query);
            result = api.getOriginalJson();
            url2jsonCache.put(uri.toString(), StringCompress.compress(result.toString()));
            increaseFlushCounter();
        }

        if (api.recacheNeeded(result) && retryLeft > 0)
            return query(query, retryLeft - 1);

        return api.buildResponseFromJson(result);
    }

    /**
     * Set the file to which the responses cache is bound.
     * 
     * @param cacheFilename
     *            the cache file name.
     * @throws FileNotFoundException
     *             if the file could not be open for reading.
     * @throws IOException
     *             if something went wrong while reading the file.
     * @throws ClassNotFoundException
     *             is the file contained an object of the wrong class.
     */
    public void setCache(String cacheFilename) throws FileNotFoundException, IOException, ClassNotFoundException {
        if (resultsCacheFilename != null && resultsCacheFilename.equals(cacheFilename))
            return;
        System.out.println("Loading websearch cache...");
        resultsCacheFilename = cacheFilename;
        if (new File(resultsCacheFilename).exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(resultsCacheFilename));
            url2jsonCache = (HashMap<String, byte[]>) ois.readObject();
            ois.close();
        }
    }

    /**
     * Clear the response cache and call the garbage collector.
     */
    public void unSetCache() {
        url2jsonCache = new HashMap<>();
        System.gc();
    }

    /**
     * Add all records contained in the cache passed by argument to the static cache, overwriting in case of conflicting keys.
     * 
     * @param newCache
     *            the cache whose records are added.
     */
    public void mergeCache(HashMap<String, byte[]> newCache) {
        for (String key : newCache.keySet()) {
            url2jsonCache.put(key, newCache.get(key));
            flushCounter++;
        }
    }

    public synchronized void flush() throws FileNotFoundException, IOException {
        if (flushCounter > 0 && resultsCacheFilename != null) {
            System.out.println("Flushing Websearch cache...");
            new File(resultsCacheFilename).createNewFile();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(resultsCacheFilename));
            oos.writeObject(url2jsonCache);
            oos.close();
            System.out.println("Flushing Websearch cache done.");
        }
    }

    @Override
    public URI getQueryURI(String query) throws URISyntaxException {
        return api.getQueryURI(query);
    }
}