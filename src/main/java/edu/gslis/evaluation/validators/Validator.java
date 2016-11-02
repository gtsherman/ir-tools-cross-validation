package edu.gslis.evaluation.validators;

import java.util.Random;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.queries.GQueries;
import edu.gslis.searchhits.SearchHitsBatch;

public abstract class Validator {

	protected int numResults;
	
	protected QueryRunner runner;
	
	public Validator(QueryRunner runner) {
		this.runner = runner;
	}
	
	public void setNumResults(int numResults) {
		this.numResults = numResults;
	}
	
	public SearchHitsBatch evaluate(GQueries queries, Evaluator evaluator) {
		Random r = new Random();
		return evaluate(r.nextLong(), queries, evaluator);
	}
	
	public abstract SearchHitsBatch evaluate(long seed, GQueries queries, Evaluator evaluator);
	
}
