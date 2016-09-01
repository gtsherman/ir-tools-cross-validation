package edu.gslis.evaluation.evaluators;

import edu.gslis.eval.Qrels;
import edu.gslis.searchhits.SearchHitsBatch;

public interface Evaluator {
	
	public double evaluate(SearchHitsBatch batchResults, Qrels qrels);

}
