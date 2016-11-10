package it.unipi.di.acube;

import java.io.*;
import java.net.*;
import java.util.*;

import org.codehaus.jettison.json.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class BingInterface {
	private static int flushCounter = 0;
	private String bingKey;
	private static final int BING_RETRY = 3;
	private static final int FLUSH_EVERY = 50;
	private static HashMap<String, byte[]> url2jsonCache = new HashMap<>();
	private static String resultsCacheFilename;

	/**
	 * @param bingKey the key that will be used to access the Bing API. For cache-only usage, you may set this value tu null.
	 */
	public BingInterface(String bingKey) {
		this.bingKey = bingKey;
	}

	private synchronized void increaseFlushCounter() throws FileNotFoundException, IOException {
		flushCounter++;
		if (flushCounter % FLUSH_EVERY == 0)
			flush();
	}

	/**
	 * @param bingReply
	 *            Bing's reply.
	 * @return whether the query to bing failed and has to be re-issued.
	 * @throws JSONException
	 *             if the Bing result could not be read.
	 */
	private static boolean recacheNeeded(JSONObject bingReply) throws JSONException {
		if (bingReply == null)
			return true;
		JSONObject data = (JSONObject) bingReply.get("d");
		if (data == null)
			return true;
		JSONObject results = (JSONObject) ((JSONArray) data.get("results")).get(0);
		if (results == null)
			return true;
		JSONArray webResults = (JSONArray) results.get("Web");
		if (webResults == null)
			return true;
		if (((String) results.get("WebTotal")).equals(""))
			return true;
		return false;
	}

	/**
	 * Issue the query to bing, return the json object.
	 * 
	 * @param query
	 *            the query.
	 * @param retryLeft
	 *            how many retry left we have (if zero, will return an empty
	 *            object in case of failure).
	 * @return the JSON object as returned by the Bing Api.
	 * @throws Exception
	 *             is the call to the API failed.
	 */
	public synchronized JSONObject queryBing(String query) throws Exception {
		return queryBing(query, BING_RETRY);
	}

	private synchronized JSONObject queryBing(String query, int retryLeft) throws Exception {
		boolean forceCacheOverride = retryLeft < BING_RETRY;
		if (forceCacheOverride)
			Thread.sleep(1000);
		String accountKeyAuth = Base64.encode((bingKey + ":" + bingKey).getBytes(), 0);

		String url = "https://api.datamarket.azure.com/Bing/Search/v1/Composite?Sources=%27web%2Bspell%2BRelatedSearch%27&Query=%27"
				+ URLEncoder.encode(query, "utf8")
				+ "%27&Options=%27EnableHighlighting%27&Market=%27en-US%27&Adult=%27Off%27&$format=Json";

		JSONObject result = null;
		byte[] compressed = url2jsonCache.get(url);
		if (compressed != null)
			result = new JSONObject(StringCompress.decompress(compressed));

		boolean cached = !forceCacheOverride && result != null;
		System.out.printf("%s%s %s%n", forceCacheOverride ? "<forceCacheOverride>" : "", cached ? "<cached>"
				: "Querying", url);
		if (!cached) {
			HttpGet get = new HttpGet(url);
			get.setHeader("Authorization", "Basic " + accountKeyAuth);
			get.setHeader("Accept", "*/*");
			get.setHeader("Content-Type", "multipart/form-data");

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpResponse response = httpClient.execute(get);

			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.printf("Got HTTP error %d. Message is: %s%n", response.getStatusLine().getStatusCode(), IOUtils.toString(response.getEntity().getContent(), "utf-8"));
				throw new RuntimeException("Got response code:" + response.getStatusLine().getStatusCode());
			}
			result = new JSONObject(IOUtils.toString(response.getEntity().getContent(), "utf-8"));
			url2jsonCache.put(url, StringCompress.compress(result.toString()));
			increaseFlushCounter();
		}

		if (recacheNeeded(result) && retryLeft > 0)
			return queryBing(query, retryLeft - 1);

		return result;
	}

	/**
	 * Set the file to which the Bing responses cache is bound.
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
	public static void setCache(String cacheFilename) throws FileNotFoundException, IOException, ClassNotFoundException {
		if (resultsCacheFilename != null && resultsCacheFilename.equals(cacheFilename))
			return;
		System.out.println("Loading bing cache...");
		resultsCacheFilename = cacheFilename;
		if (new File(resultsCacheFilename).exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(resultsCacheFilename));
			url2jsonCache = (HashMap<String, byte[]>) ois.readObject();
			ois.close();
		}
	}

	/**
	 * Clear the Bing response cache and call the garbage collector.
	 */
	public static void unSetCache() {
		url2jsonCache = new HashMap<>();
		System.gc();
	}

	/**
	 * Add all records contained in the cache passed by argument to the static
	 * cache, overwriting in case of conflicting keys.
	 * 
	 * @param newCache
	 *            the cache whose records are added.
	 */
	public static void mergeCache(HashMap<String, byte[]> newCache) {
		for (String key : newCache.keySet()) {
			url2jsonCache.put(key, newCache.get(key));
			flushCounter++;
		}
	}

	public static synchronized void flush() throws FileNotFoundException, IOException {

		if (flushCounter > 0 && resultsCacheFilename != null) {
			System.out.print("Flushing Bing cache... ");
			new File(resultsCacheFilename).createNewFile();
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(resultsCacheFilename));
			oos.writeObject(url2jsonCache);
			oos.close();
			System.out.println("Flushing Bing cache Done.");
		}
	}
}
