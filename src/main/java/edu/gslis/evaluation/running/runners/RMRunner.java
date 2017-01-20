package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Map;

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

public class RMRunner extends QueryRunner {

	public static final String ORIG_QUERY_WEIGHT = "original";
	public static final String FEEDBACK_DOCUMENTS = "fbDocs";
	public static final String FEEDBACK_TERMS = "fbTerms";
	
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	
	public RMRunner(IndexWrapperIndriImpl index, Stopper stopper) {
		this.index = index;
		this.stopper = stopper;
	}
	
	@Override
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();
		
		currentParams.put(FEEDBACK_DOCUMENTS, 20.0);
		currentParams.put(FEEDBACK_TERMS, 20.0);
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			
			currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, origWeight);
			
			SearchHitsBatch batchResults = run(queries, NUM_TRAINING_RESULTS, currentParams);
			
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
	
	@Override
	public SearchHits runQuery(QueryParameters queryParams) {
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
		
		RM1Builder rm1 = new StandardRM1Builder(fbDocs, fbTerms, collectionStats);
		RM3Builder rm3 = new RM3Builder();
		FeatureVector rm3Vector = rm3.buildRelevanceModel(query, initialHits, rm1, params.get(ORIG_QUERY_WEIGHT), stopper);
		
		GQuery newQuery = new GQuery();
		newQuery.setTitle(query.getTitle());
		newQuery.setFeatureVector(rm3Vector);
		
		return index.runQuery(newQuery, numResults);
	}

}
