package it.unipi.di.acube.searchapi.model;

import java.util.Date;

public class WebsearchResponseEntry {
    String name, displayUrl, snippet;
    Date lastCrawled;

    public WebsearchResponseEntry(String name, String displayUrl, String snippet, Date dateLastCrawled) {
        this.name = name;
        this.displayUrl = displayUrl;
        this.snippet = snippet;
        this.lastCrawled = dateLastCrawled;
    }

    /**
     * @return the page title.
     */
    public String getName() {
        return name;
    }

    /**
     * @return display URL. Note that results highlighting applies to the display URL.
     */
    public String getDisplayUrl() {
        return displayUrl;
    }

    /**
     * @return the snippet of text drawn from the result. Note that results highlighting applies to the snippet.
     */
    public String getSnippet() {
        return snippet;
    }

    /**
     * @return date-time when the document was last crawled. Can be null if not specified by the API response.
     */
    public Date getLastCrawled() {
        return lastCrawled;
    }
}
