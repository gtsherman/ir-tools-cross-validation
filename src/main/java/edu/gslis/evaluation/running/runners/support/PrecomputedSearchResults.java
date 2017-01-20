package edu.gslis.evaluation.running.runners.support;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.data.interpreters.SearchResultsDataInterpreter;
import edu.gslis.utils.data.sources.FileDataSource;

/**
 * Store precomputed search results batches by parameter settings.
 * 
 * @author Garrick
 * 
 */
public class PrecomputedSearchResults implements Iterable<String> {
	
	private Map<String, SearchHitsBatch> results;
	private SearchResultsDataInterpreter interpreter;

	public PrecomputedSearchResults() {
		results = new HashMap<String, SearchHitsBatch>();
		interpreter = new SearchResultsDataInterpreter();
	}
	
	public void addResults(File file) {
		if (file.isDirectory()) {
			return;
		}

		FileDataSource data = new FileDataSource(file);
		SearchHitsBatch batchResults = interpreter.build(data);
		results.put(file.getName(), batchResults);
	}
	
	public void addResults(File... files) {
		for (File file : files) {
			addResults(file);
		}
	}
	
	public SearchHitsBatch getSearchHitsBatch(String key) {
		return results.get(key);
	}
	
	@Override
	public Iterator<String> iterator() {
		return results.keySet().iterator();
	}

}
