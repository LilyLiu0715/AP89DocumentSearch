package com.ir.ap89.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * A class that represents a search result.
 */
public class SearchResult {
    private static final int NUM_SCORE_DECIMALS = 3;
    
    private Document document;
    private double score;
    private Map<String, List<String>> highlights;

    public SearchResult(Document document, double score, Map<String, List<String>> highlights) {
        this.document = document;
        this.score = score;
        this.highlights = highlights;
    }

    public String displayScore() {
        BigDecimal bd = new BigDecimal(Double.toString(score));
        bd = bd.setScale(NUM_SCORE_DECIMALS, RoundingMode.HALF_UP);
        return bd.toString();
    }

    public String displayDocNo() {
        return document.docNo;
    }

    public String displayFileId() {
        return document.fileId;
    }

    public List<String> displayHighlightedHead() {
        if (highlights.containsKey(Document.HEAD_FIELD_NAME)) {
            return highlights.get(Document.HEAD_FIELD_NAME);
        }

        return document.head;
    }

    public List<String> displayHighlightedByline() {
        if (highlights.containsKey(Document.BYLINE_FIELD_NAME)) {
            return highlights.get(Document.BYLINE_FIELD_NAME);
        }

        return document.byline;
    }

    public List<String> displayHighlightedText() {
        if (highlights.containsKey(Document.TEXT_FIELD_NAME)) {
            return highlights.get(Document.TEXT_FIELD_NAME);
        }

        return document.text;
    }
    
}
