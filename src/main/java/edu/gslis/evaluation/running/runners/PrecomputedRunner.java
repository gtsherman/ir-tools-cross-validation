package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.PrecomputedSearchResults;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;

/**
 * Run cross-validation using previously computed search results batches.
 * 
 * @author Garrick
 *
 */
public class PrecomputedRunner extends QueryRunner {
	
	PrecomputedSearchResults precomputed;
	
	public PrecomputedRunner(PrecomputedSearchResults precomputed) {
		this.precomputed = precomputed;
		
		// Use the maximum available search results for each query
		this.NUM_TRAINING_RESULTS = -1;
	}

	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double max = 0.0;
		String best = "";

		// Iterate over each precomputed parameter setting
		Iterator<String> it = precomputed.iterator();
		while (it.hasNext()) {
			String params = it.next();

			/**
			 * Required by the framework. The important information is actually
			 * the key, but we also need a hashed double to identify this run
			 * in the cache. The QueryParameters object that will make use of
			 * this value can handle any hash collisions. 
			 */
			Map<String, Double> paramsMap = new HashMap<String, Double>();
			paramsMap.put(params, new Double(params.hashCode()));

			SearchHitsBatch toScore = run(queries, NUM_TRAINING_RESULTS, paramsMap);
			
			double score = evaluator.evaluate(toScore);
			if (score > max) {
				best = params;
				max = score;
			}
		}
		
		Map<String, Double> bestParams = new HashMap<String, Double>();
		bestParams.put(best, new Double(best.hashCode()));
		return bestParams;
	}

	@Override
	public SearchHits runQuery(QueryParameters queryParams) {
		GQuery query = queryParams.getQuery();
		int numResults = queryParams.getNumResults();
		Map<String, Double> params = queryParams.getParams();
		
		/**
		 *  Since each parameter setting is represented by a single name, there
		 *  is only one key, so retrieve that.
		 */
		String actualParams = params.keySet().iterator().next();
		
		SearchHitsBatch all = precomputed.getSearchHitsBatch(actualParams);
		SearchHits results = all.getSearchHits(query);
		
		if (results == null) {
			System.err.println("No results for " + query.getTitle() + " in " +
					actualParams);
			results = new SearchHits();
		}
		
		/** 
		 * If for some reason we want to use less than the supplied results, we
		 * should create a new SearchHits to hold them, since cropping the
		 * original results may cause problems if we later want to use the full
		 * supplied results.
		 */
		if (numResults > -1) {
			if (numResults < results.size()) {
				SearchHits full = results;
				results = new SearchHits();

				int i = 0;
				Iterator<SearchHit> hitit = full.iterator();
				while (hitit.hasNext() && i < numResults) {
					results.add(hitit.next());
					i++;
				}
			} else if (numResults > results.size()){
				System.err.println("Requested number of results (" + numResults +
						") is greater than the total number available (" +
						results.size() + ") for query " + query.getTitle() +
						". Using total available.");
			}
		}
		
		return results;
	}

}
