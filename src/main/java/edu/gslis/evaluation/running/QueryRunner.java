package edu.gslis.evaluation.running;

import java.util.Map;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.queries.GQueries;
import edu.gslis.searchhits.SearchHitsBatch;

public interface QueryRunner {

	/**
	 * Sweep over queries, returning optimal parameter settings for given evaluation metric
	 * @param queries
	 * @param evaluator	An Evaluator object representing the metric to optimize
	 * @param qrels	A Qrels object containing the relevance judgments for the given queries
	 * @return	A set of optimal parameter values
	 */
	public Map<String, Double> sweep(GQueries queries, Evaluator evaluator, Qrels qrels);
	
	/**
	 * Execute the queries
	 * @param queries
	 * @param numResults The maximum number of documents to return per query
	 * @param params Any parameters with their values to be used when running the queries
	 * @return	Batch results of running each of the given queries with the given parameters
	 */
	public SearchHitsBatch run(GQueries queries, int numResults, Map<String, Double> params);
	
}
