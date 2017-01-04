package edu.gslis.evaluation.running;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;

public abstract class QueryRunner {

	/**
	 * The number of search results to retrieve during training runs. By default, use a low number
	 * to improve performance.
	 * 
	 * <p>Setting this value as a variable allows us to be smarter about caching search results; any
	 * result list not equal to <code>NUM_TRAINING_RESULTS</code> in size will likely not be seen
	 * again and therefore need not be cached.
	 */
	public int NUM_TRAINING_RESULTS = 100;
	
	/**
	 * The cache of query results. By default, this uses soft values (allowing the garbage collector
	 * to handle cleanup) and calls the <code>runQuery</code> method to handle actual query
	 * processing logic.
	 */
	private LoadingCache<QueryParameters, SearchHits> processedQueries = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<QueryParameters, SearchHits>() {
						public SearchHits load(QueryParameters queryParams) throws Exception {
							return runQuery(queryParams);
						}
					});
	
	/**
	 * The cache's getter method
	 * 
	 * <p>Override this method to return a different cache if the default is not acceptable.
	 * You can do this by creating a local, private <code>LoadingCache</code>
	 * instance variable in your subclass, and then return that by your subclass'
	 * <code>getCache</code> method.
	 * 
	 * @return The cache
	 */
	public LoadingCache<QueryParameters, SearchHits> getCache() {
		return processedQueries;
	}
	
	/**
	 * Sweep over queries, returning optimal parameter settings for given evaluation metric
	 * 
	 * <p>This method is designed to call the <code>run</code> method repeatedly, for 
	 * each possible parameter combination. Using the supplied <code>Evaluator</code>,
	 * this method can keep track of the performance of each parameter setting, ultimately
	 * returning the parameter settings that performed best.
	 * 
	 * @param queries The batch of queries to run
	 * @param evaluator	An <code>Evaluator</code> object representing the metric to optimize
	 * @return	A set of optimal parameter values
	 */
	public abstract Map<String, Double> sweep(GQueries queries, Evaluator evaluator);
	
	/**
	 * Execute the queries
	 * 
	 * <p>By default, this method simply iterates over queries, getting the search results
	 * for each from the cache and adding them to the batch results to be returned. 
	 * Override this method if you require different behavior.
	 * 
	 * @param queries The batch of queries
	 * @param numResults The maximum number of documents to return per query
	 * @param params Any parameters with their values to be used when running the queries
	 * @return	Batch results of running each of the given queries with the given parameters
	 */
	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();

		for (GQuery query : queries) {
			QueryParameters queryParams = new QueryParameters(query, numResults, params);

			SearchHits results;
			try {
				if (numResults == NUM_TRAINING_RESULTS) {
					// Get via cache
					results = getCache().get(queryParams);
				} else {
					// Non-training runs do not need to be cached; process fresh
					results = runQuery(queryParams);
				}
			} catch (ExecutionException e) {
				System.err.println("Error scoring query " + queryParams.getQuery().getTitle());
				System.err.println(e.getStackTrace());
				results = new SearchHits();
			}
			batchResults.setSearchHits(query.getTitle(), results);
		}

		return batchResults;
	}
	
	/**
	 * The core code to execute a single query with given parameters
	 * 
	 * <p>This method will likely contain the core code for an experiment, since it handles
	 * the actual retrieval for a given query with specified parameter settings.
	 * 
	 * @param queryParams Contains the query to be executed and the parameters to use
	 * @return The search results returned for the given query with the given parameters
	 */
	public abstract SearchHits runQuery(QueryParameters queryParams);
	
}
