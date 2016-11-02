package edu.gslis.evaluation.evaluators;

import edu.gslis.eval.Qrels;
import edu.gslis.searchhits.SearchHitsBatch;

public abstract class Evaluator {
	
	protected Qrels qrels;
	
	public Evaluator(Qrels qrels) {
		this.qrels = qrels;
	}
	
	public abstract double evaluate(SearchHitsBatch batchResults);

}
