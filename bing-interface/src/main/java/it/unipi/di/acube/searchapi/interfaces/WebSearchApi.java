package it.unipi.di.acube.searchapi.interfaces;

import java.net.URI;
import java.net.URISyntaxException;

import it.unipi.di.acube.searchapi.model.WebsearchResponse;

public interface WebSearchApi {
    public static final char SNIPPET_BOLD_START = 0xe000;
    public static final char SNIPPET_BOLD_END = 0xe001;
    public static final String SNIPPET_BOLD_START_STR = new String(Character.toString(SNIPPET_BOLD_START));
    public static final String SNIPPET_BOLD_END_STR = new String(Character.toString(SNIPPET_BOLD_END));

    URI getQueryURI(String query) throws URISyntaxException;

    WebsearchResponse query(String query) throws Exception;

}
