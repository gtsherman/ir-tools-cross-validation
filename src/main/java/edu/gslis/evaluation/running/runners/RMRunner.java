package edu.gslis.evaluation.running.runners;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import edu.gslis.utils.readers.RelevanceModelReader;

public class RMRunner implements QueryRunner {

	public static final String ORIG_QUERY_WEIGHT = "original";
	public static final String FEEDBACK_DOCUMENTS = "fbDocs";
	public static final String FEEDBACK_TERMS = "fbTerms";
	
	private IndexWrapperIndriImpl index;
	private String rmsDir;
	private Stopper stopper;
	
	private Map<GQuery, FeatureVector> queryRMs;
	
	public RMRunner(IndexWrapperIndriImpl index, Stopper stopper) {
		this(index, stopper, null);
	}
	
	public RMRunner(IndexWrapperIndriImpl index, Stopper stopper, String rmsDir) {
		this.index = index;
		this.stopper = stopper;
		this.rmsDir = rmsDir;
	}
	
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator) {
		double maxMetric = 0.0;

		Map<String, Double> bestParams = new HashMap<String, Double>();
		Map<String, Double> currentParams = new HashMap<String, Double>();
		
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
		// Read the RMs and store them so we don't have to do it each time this is run,
		// but implies that fbdocs and fbterms will not change.
		if (rmsDir != null) {
			readRMs(queries);
		}

		SearchHitsBatch batchResults = new SearchHitsBatch();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			query.applyStopper(stopper);

			FeatureVector rmVec;
			if (rmsDir != null) {
				rmVec = queryRMs.get(query);
			} else {
				FeedbackRelevanceModel rm1 = new FeedbackRelevanceModel();
				try {
					rm1.setDocCount(params.get(FEEDBACK_DOCUMENTS).intValue());
				} catch (NullPointerException e) {
					rm1.setDocCount(20);
				}
				try {
					rm1.setTermCount(params.get(FEEDBACK_TERMS).intValue());
				} catch (NullPointerException e) {
					rm1.setTermCount(20);
				}
				rm1.setIndex(index);
				rm1.setStopper(stopper);
				rm1.setOriginalQuery(query);
				
				rm1.build();
				rmVec = rm1.asGquery().getFeatureVector();
				rmVec.normalize();
			}

			FeatureVector rm3 = FeatureVector.interpolate(query.getFeatureVector(), rmVec, params.get(ORIG_QUERY_WEIGHT));
			
			GQuery newQuery = new GQuery();
			newQuery.setTitle(query.getTitle());
			newQuery.setFeatureVector(rm3);
			
			SearchHits results = index.runQuery(newQuery, numResults);
			batchResults.setSearchHits(query.getTitle(), results);
		}
		return batchResults;
	}

	private void readRMs(GQueries queries) {
		Map<GQuery, FeatureVector> queryRMs = new HashMap<GQuery, FeatureVector>();
		
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext()) {
			GQuery query = queryIt.next();
			if (!queryRMs.containsKey(query)) {
				RelevanceModelReader reader = new RelevanceModelReader(new File(rmsDir+File.pathSeparator+query.getTitle()));
				queryRMs.put(query, reader.getFeatureVector());
			}
		}
		
		this.queryRMs = queryRMs;
	}

}
