package edu.gslis.evaluation.validators;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.queries.GQueries;
import edu.gslis.searchhits.SearchHitsBatch;

public class HoldoutValidator extends Validator {

	public HoldoutValidator(QueryRunner runner) {
		super(runner);
	}
	
	@Override
	public SearchHitsBatch evaluate(long seed, GQueries queries, Evaluator evaluator, Qrels qrels) {
		KFoldValidator validator = new KFoldValidator(runner);
		validator.setNumFolds(2);
		return validator.evaluate(seed, queries, evaluator, qrels);
	}

}
