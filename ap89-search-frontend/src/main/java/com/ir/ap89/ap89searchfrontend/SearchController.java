package com.ir.ap89.ap89searchfrontend;

import java.util.List;
import java.util.logging.Logger;

import com.ir.ap89.model.SearchResult;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {    
    private static final Logger LOGGER = Logger.getLogger(SearchController.class.getName());

    private final Searcher searcher;

    public SearchController(Searcher searcher) {
        this.searcher = searcher;
    }

    @GetMapping(value="/search")
    public String runQuery(@RequestParam(value = "query") String query,
                           @RequestParam(value = "startRank", defaultValue = "0") Integer startRank,
                           Model model) {
        LOGGER.info(String.format(
            "Received request for running query \"%s\" with startRank %d", query, startRank));
        model.addAttribute("query", query);
        model.addAttribute("startRank", startRank);

        List<SearchResult> results = searcher.search(query, startRank);

        model.addAttribute("results", results);
        model.addAttribute("resultSize", results.size());
        
        return "search";
    }
    
}
