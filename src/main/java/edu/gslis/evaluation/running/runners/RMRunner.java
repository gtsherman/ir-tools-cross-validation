package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.queries.expansion.FeedbackRelevanceModel;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.textrepresentation.FeatureVector;
import edu.gslis.utils.Stopper;

public class RMRunner implements QueryRunner {

	public static final String ORIG_QUERY_WEIGHT = "original";
	
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	private int fbDocs;
	private int fbTerms;
	
	private Map<GQuery, FeatureVector> queryRMs;
	
	public RMRunner(IndexWrapperIndriImpl index, Stopper stopper) {
		this(index, stopper, 20, 20);
	}
	
	public RMRunner(IndexWrapperIndriImpl index, Stopper stopper, int fbDocs, int fbTerms) {
		this.index = index;
		this.stopper = stopper;
		this.fbDocs = fbDocs;
		this.fbTerms = fbTerms;
	}
	
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator, Qrels qrels) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();
		
		for (int origW = 0; origW <= 10; origW++) {
			double origWeight = origW / 10.0;
			
			currentParams.put(RMRunner.ORIG_QUERY_WEIGHT, origWeight);

			SearchHitsBatch batchResults = run(queries, 1000, currentParams);
			
			double metricVal = evaluator.evaluate(batchResults, qrels);
			if (metricVal > maxMetric) {
				maxMetric = metricVal;
				bestParams.putAll(currentParams);
			}
		}
		
		return bestParams;
	}

	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		// Build the RMs and store them so we don't have to do it each time this is run
		if (queryRMs == null) {
			precomputeRMs(queries, fbDocs, fbTerms);
		}

		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			
			FeatureVector rmVec = queryRMs.get(query);
			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), rmVec, params.get(ORIG_QUERY_WEIGHT));
			
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3);
			
			SearchHits results = index.runQuery(newQuery, numResults);
			batchResults.setSearchHits(query.getTitle(), results);
		}
		return batchResults;
	}
	
	private Map<GQuery, FeatureVector> precomputeRMs(GQueries queries, int fbDocs, int fbTerms) {
		Map<GQuery, FeatureVector> queryRMs = new HashMap<GQuery, FeatureVector>();
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			FeedbackRelevanceModel rm1 = new FeedbackRelevanceModel();
			rm1.setDocCount(20);
			rm1.setTermCount(20);
			rm1.setIndex(index);
			rm1.setStopper(stopper);
			rm1.setOriginalQuery(query);
			
			rm1.build();
			
			FeatureVector rmVec = rm1.asGquery().getFeatureVector();
			rmVec.normalize();
			
			queryRMs.put(query, rmVec);
		}
		
		return queryRMs;
	}

}
