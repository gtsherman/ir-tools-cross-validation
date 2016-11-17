package edu.gslis.evaluation.running.runners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.support.ParameterizedResults;
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
	public static final String FEEDBACK_DOCUMENTS = "fbDocs";
	public static final String FEEDBACK_TERMS = "fbTerms";
	
	private IndexWrapperIndriImpl index;
	private Stopper stopper;
	
	private ParameterizedResults processedQueries = new ParameterizedResults();
	
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
		
		return bestParams;
	}

	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params) {
		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			SearchHits results = getProcessedQuery(query, numResults, params);
			batchResults.setSearchHits(query.getTitle(), results);
		}
		return batchResults;
	}
	
	private SearchHits getProcessedQuery(GQuery query, int numResults, Map<String, Double> params) {
		double[] paramVals = {params.get(ORIG_QUERY_WEIGHT), params.get(FEEDBACK_DOCUMENTS), params.get(FEEDBACK_TERMS), numResults};
		
		if (!processedQueries.resultsExist(query, paramVals)) {
			query.applyStopper(stopper);

			int fbDocs = 20;
			if (params.containsKey(FEEDBACK_DOCUMENTS)) {
				fbDocs = params.get(FEEDBACK_DOCUMENTS).intValue();
			}
			int fbTerms = 20;
			if (params.containsKey(FEEDBACK_TERMS)) {
				fbTerms = params.get(FEEDBACK_TERMS).intValue();
			}

			FeedbackRelevanceModel rm1 = new FeedbackRelevanceModel();
			rm1.setDocCount(fbDocs);
			rm1.setTermCount(fbTerms);
			rm1.setIndex(index);
			rm1.setStopper(stopper);
			rm1.setOriginalQuery(query);
			rm1.build();

			FeatureVector rmVec = rm1.asGquery().getFeatureVector();
			rmVec.normalize();

			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), rmVec, params.get(ORIG_QUERY_WEIGHT));
			
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3);
			
			SearchHits results = index.runQuery(newQuery, numResults);
			processedQueries.addResults(results, query, paramVals);
		}

		return processedQueries.getResults(query, paramVals);
	}

}
