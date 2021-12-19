package com.ir.ap89.ap89searchfrontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ir.ap89.model.Document;
import com.ir.ap89.model.SearchResult;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.base.Transport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._core.SearchRequest;
import co.elastic.clients.elasticsearch._core.SearchResponse;
import co.elastic.clients.elasticsearch._core.search.Highlight;
import co.elastic.clients.elasticsearch._core.search.HighlightField;
import co.elastic.clients.elasticsearch._core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import jakarta.json.Json;
import jakarta.json.JsonArray;

/**
 * This class searches relevant AP89 documents from the cloud ElasticSearch instance
 * based on the given query text.
 */
@Component
public class Searcher {
    private static final Logger LOGGER = Logger.getLogger(Searcher.class.getName());

    private static final int SEARCH_RESULT_PAGE_SIZE = 10;

    // Elastic search related constants.
    private static final String INDEX_NAME = "ap89-index";
    private static final String HOST_NAME = "ir-projects.es.us-central1.gcp.cloud.es.io";
    private static final int PORT_NUMBER = 9243;
    private static final String USER_NAME = "elastic";
    private static final String PASSWORD = "sYCAq2PuJqsEUyKH4EW7rMzS";

    private final ElasticsearchClient client;

    public Searcher() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(USER_NAME, PASSWORD));
        RestClient restClient = RestClient.builder(
                new HttpHost(HOST_NAME, PORT_NUMBER, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)).build();
        JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
        Transport transport = new RestClientTransport(restClient, jsonMapper);
        this.client = new ElasticsearchClient(transport);
    }

    /**
     * Search for releant documents from the cloud ES instance.
     * @param queryText the user provided query text.
     * @param startRank the start rank for search result.
     * @return a list of relevant search results.
     */
    public List<SearchResult> search(String queryText, int startRank) {
        List<SearchResult> results = new ArrayList<>();
        SearchRequest request = new SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(new Query.Builder().multiMatch(getMultiMatchQuery(queryText)).build())
            .sort(getSortOption())
            .highlight(getHighlightOption())
            .from(startRank)
            .size(SEARCH_RESULT_PAGE_SIZE)
            .build();
        
        try {
            SearchResponse<Document> response = client.search(request, Document.class);
            List<Hit<Document>> hits = response.hits().hits();
            for (Hit<Document> hit : hits) {
                SearchResult result = new SearchResult(hit.source(), hit.score(), hit.highlight());
                results.add(result);
            }
            LOGGER.info("Receiving search response: " + response);
        } catch (IOException e) {
            LOGGER.severe("Failed to search ElasticSearch. Error: " +e.getMessage());
        }

        return results;
    }

    /**
     * Construct a multi-match query that searches comment and video name fields.
     * @param queryText the given query text.
     * @return the multi-match query.
     */
    private MultiMatchQuery getMultiMatchQuery(String queryText) {
        // Enable boosting on less noisy fields
        List<String> fields = new ArrayList<>();
        for (String field : Document.QUERIABLE_FIELD_NAMES) {
            if (Document.LESS_NOISY_FIELD_NAMES.contains(field)) {
                fields.add(field + "^2");
            } else {
                fields.add(field);
            }
        }
        return new MultiMatchQuery.Builder()
            .query(queryText)
            .fields(fields)
            .fuzziness("AUTO")  // Enable fuzziness.
            .analyzer("standard_with_custom_stopwords")  // Enable filtering out stop words.
            .build();
    }

    /**
     * Construct a sort option to sort by both relevance score
     * and number of likes.
     * @return the sort option in JSON format.
     */
    private JsonArray getSortOption() {
        return Json.createArrayBuilder()
            .add("_score")
            .build();
    }

    /**
     * Construct a highlight option to highlight matching text
     * in comment and video name fields.
     * @return the highlight option.
     */
    private Highlight getHighlightOption() {
        Map<String, HighlightField> highlightFields = new HashMap<>();
        // Set number of fragments to 0 to avoid breaking short text fields into segments.
        for (String fieldName : Document.QUERIABLE_FIELD_NAMES) {
            highlightFields.put(
                fieldName, new HighlightField.Builder().numberOfFragments(fieldName == Document.TEXT_FIELD_NAME ? 5 : 0).build());
        }
        return new Highlight.Builder().fields(highlightFields).build();
    }
}
