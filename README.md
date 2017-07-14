Bing and Google CSE API library for Java
=============

A small set of Java classes to query the Bing Search and the Google CSE (Custom Search Engine) APIs.

The library is published on Maven Central, you can include it as a dependency in your `pom.xml` file as:
```
<dependency>
	<groupId>it.unipi.di.acube</groupId>
	<artifactId>bing-api-java</artifactId>
</dependency>
```

# Registration to the APIs
*By using this library you accept that it may issue  any number of calls to the search engines API on your account, which may results in spending your credit or money.*

- To use the Google CSE API, you need to setup a [Google Custom Search Engine](https://developers.google.com/custom-search/) and create an API key from the Google Developers Console. For an example of building a generic CSE for the whole web, see [here](https://sobigdata.d4science.org/group/smaph/documentation) (Sections "Setting up Google CSE" and "Enabling the Google API").
- To use the Bing API, register to the [Bing Web Search API](https://azure.microsoft.com/en-us/pricing/details/cognitive-services/search-api/).


# Usage
1- Create a `SearchApiCaller` for either Bing:
```
BingSearchApiCaller caller = new BingSearchApiCaller("<MY_BING_KEY>");
```
or Google CSE:
```
GoogleSearchApiCaller caller = new GoogleSearchApiCaller("<MY_CSE_ID>", "<MY_GOOGLE_API_KEY>");
```

2- Create a wrapper for the caller that will take care of performing multiple-calls queries:
```
WebsearchApi api = new WebsearchApi(caller);
```
Alternatively, if you need result caching, you may create a cached wrapper:
```
CachedWebsearchApi api = CachedWebsearchApi.builder().api(caller).path("<CACHE_FILE_NAME>").create();
```
Cached wrappers will store query responses in a file and will not re-issue the same query twice.

3- Now you can issue calls with:
```
WebsearchResponse response = api.query("QUERY_TEXT", NUMBER_OF_RESULTS_NEEDED);
```

`response` will contain all information returned by the search engine's API.

You can see a full example of a command-line script for [Bing](bing-interface/src/main/java/it/unipi/di/acube/searchapi/main/QueryBing.java) or [Google](bing-interface/src/main/java/it/unipi/di/acube/searchapi/main/QueryGoogle.java).

# Contacts
For any bug you encounter, you can open a bug report on [github](https://github.com/marcocor/bing-api-java/issues).

For any enquiry, send an email at x at di.unipi.it (replace x with 'cornolti')

Enjoy,
Marco
