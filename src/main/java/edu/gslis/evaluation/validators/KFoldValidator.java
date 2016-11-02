package edu.gslis.evaluation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHitsBatch;

public class KFoldValidator extends Validator {
	
	protected int k;

	public KFoldValidator(QueryRunner runner) {
		this(runner, 10, 1000);
	}
	
	public KFoldValidator(QueryRunner runner, int k) {
		this(runner, k, 1000);
	}

	public KFoldValidator(QueryRunner runner, int k, int numResults) {
		super(runner);
		setNumFolds(k);
		setNumResults(numResults);
	}
	
	public void setNumFolds(int k) {
		this.k = k;
	}

	public SearchHitsBatch evaluate(long seed, GQueries queries, Evaluator evaluator) {
		List<GQuery> queryList = new ArrayList<GQuery>();
		Iterator<GQuery> queryIt = queries.iterator();
		while (queryIt.hasNext())
			queryList.add(queryIt.next());

		// Partition the dataset
		Collections.shuffle(queryList, new Random(seed));
		List<List<GQuery>> queryChunks = new ArrayList<List<GQuery>>();
		for (int i = 0; i < queryList.size(); i++) {
			int c = i % k; // the chunk to use

			if (queryChunks.size() <= c)
				queryChunks.add(c, new ArrayList<GQuery>());

			queryChunks.get(c).add(queryList.get(i));
		}
		
		System.err.println("Split into "+queryChunks.size()+" chunks.");
		
		SearchHitsBatch batchResults = new SearchHitsBatch();
		
		// Run the evaluation
		for (int t = 0; t < k; t++) { // t is the test chunk
			System.err.println("Running fold "+(t+1)+"/"+k);
			
			// Set up training queries
			GQueries trainingQueries = new GQueriesJsonImpl();
			for (int i = 0; i < k; i++) {
				if (i == t)
					continue;
				
				for (GQuery query : queryChunks.get(i))
					trainingQueries.addQuery(query);
			}
			System.err.println("\tAdded "+trainingQueries.numQueries()+" training queries");
			
			// Train
			System.err.println("\tTraining...");
			Map<String, Double> parameters = runner.sweep(trainingQueries, evaluator);

			// Set up testing queries
			GQueries testingQueries = new GQueriesJsonImpl();
			for (GQuery query : queryChunks.get(t))
				testingQueries.addQuery(query);

			// Test
			System.err.println("\tTesting...");
			SearchHitsBatch b = runner.run(testingQueries, numResults, parameters);
			batchResults.addBatchResults(b);
		}
		
		return batchResults;
	}

}
