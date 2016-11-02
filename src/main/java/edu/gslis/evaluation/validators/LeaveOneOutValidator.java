package edu.gslis.evaluation.validators;

import edu.gslis.evaluation.evaluators.Evaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.queries.GQueries;
import edu.gslis.searchhits.SearchHitsBatch;

public class LeaveOneOutValidator extends Validator {
	
	public LeaveOneOutValidator(QueryRunner runner) {
		super(runner);		
	}

	@Override
	public SearchHitsBatch evaluate(long seed, GQueries queries, Evaluator evaluator) {
		KFoldValidator validator = new KFoldValidator(runner);
		validator.setNumFolds(queries.numQueries());
		return validator.evaluate(seed, queries, evaluator);
	}

}
