package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.gslis.docscoring.support.IndexBackedCollectionStats;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.QueryParameters;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.scoring.expansion.RM1Builder;
import edu.gslis.scoring.expansion.RM3Builder;
import edu.gslis.scoring.expansion.StandardRM1Builder;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;
import edu.gslis.utils.retrieval.QueryResults;

public class RMRunner implements QueryRunner {

	public static final String ORIG_QUERY_WEIGHT = "original";
	public static final String FEEDBACK_DOCUMENTS = "fbDocs";
	public static final String FEEDBACK_TERMS = "fbTerms";
	
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	
	private LoadingCache<QueryParameters, SearchHits> processedQueries = CacheBuilder.newBuilder()
			.softValues()
			.build(
					new CacheLoader<QueryParameters, SearchHits>() {
						public SearchHits load(QueryParameters queryParams) throws Exception {
							return processQuery(queryParams);
						}
					});
	
	public RMRunner(IndexWrapperIndriImpl index, Stopper stopper) {
		this.index = index;
		this.stopper = stopper;
	}
	
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();
		
		currentParams.put(FEEDBACK_DOCUMENTS, 20.0);
		currentParams.put(FEEDBACK_TERMS, 20.0);
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			
			currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, origWeight);

			SearchHitsBatch batchResults = run(queries, 1000, currentParams);
			
			double metricVal = evaluator.evaluate(batchResults);
			if (metricVal > maxMetric) {
				maxMetric = metricVal;
				bestParams.putAll(currentParams);
			}
		}

		System.err.println("Best parameters:");
		for (String param : bestParams.keySet()) {
			System.err.println(param+": "+bestParams.get(param));
		}
		return bestParams;	
	}

	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			QueryParameters queryParams = new QueryParameters(query, numResults, params);
			SearchHits results = getProcessedQuery(queryParams);
			batchResults.setSearchHits(query.getTitle(), results);
		}
		return batchResults;
	}
	
	private SearchHits getProcessedQuery(QueryParameters queryParams) {
		// Get via cache
		try {
			return processedQueries.get(queryParams);
		} catch (ExecutionException e) {
			System.err.println("Error scoring query " + queryParams.getQuery().getTitle());
			System.err.println(e.getStackTrace());
		}
		
		// Default to zero, if we have an issue
		return new SearchHits();
	}
	
	private SearchHits processQuery(QueryParameters queryParams) {
		GQuery query = queryParams.getQuery();
		int numResults = queryParams.getNumResults();
		Map<String, Double> params = queryParams.getParams();
		
		query.applyStopper(stopper);

		int fbDocs = 20;
		if (params.containsKey(FEEDBACK_DOCUMENTS)) {
			fbDocs = params.get(FEEDBACK_DOCUMENTS).intValue();
		}
		int fbTerms = 20;
		if (params.containsKey(FEEDBACK_TERMS)) {
			fbTerms = params.get(FEEDBACK_TERMS).intValue();
		}
		
		IndexBackedCollectionStats collectionStats = new IndexBackedCollectionStats();
		collectionStats.setStatSource(index);
		
		SearchHits initialHits = index.runQuery(query, fbDocs);
		
		QueryResults queryResults = new QueryResults(query, initialHits);

		RM1Builder rm1 = new StandardRM1Builder(fbDocs, fbTerms, collectionStats);
		RM3Builder rm3 = new RM3Builder();
		FeatureVector rm3Vector = rm3.buildRelevanceModel(queryResults, rm1, params.get(ORIG_QUERY_WEIGHT), stopper);
		
		GQuery newQuery = new GQuery();
		newQuery.setTitle(query.getTitle());
		newQuery.setFeatureVector(rm3Vector);
		
		return index.runQuery(newQuery, numResults);
	}

}
