package edu.gslis.evaluation.demo;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

import edu.gslis.eval.Qrels;
import edu.gslis.evaluation.evaluators.MAPEvaluator;
import edu.gslis.evaluation.running.QueryRunner;
import edu.gslis.evaluation.running.runners.RMRunner;
import edu.gslis.evaluation.validators.KFoldValidator;
import edu.gslis.indexes.IndexWrapperIndriImpl;
import edu.gslis.output.FormattedOutputTrecEval;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQueriesJsonImpl;
import edu.gslis.searchhits.SearchHitsBatch;
import edu.gslis.utils.Configuration;
import edu.gslis.utils.SimpleConfiguration;
import edu.gslis.utils.Stopper;

public class RM3CrossValidationDemo {

	public static void main(String[] args) {
		// Load configuration file
		Configuration config = new SimpleConfiguration();
		config.read(args[0]);
		
		// Read in necessary configuration
		IndexWrapperIndriImpl index = new IndexWrapperIndriImpl(config.get("index"));
		Stopper stopper = new Stopper(config.get("stoplist"));
		GQueries queries = new GQueriesJsonImpl();
		queries.read(config.get("queries"));
		Qrels qrels = new Qrels(config.get("qrels"), false, 1);

		// Setup output writer
		Writer outputWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		FormattedOutputTrecEval output = FormattedOutputTrecEval.getInstance("cross-validation", outputWriter);
		
		// Setup RMRunner to handle relevance models
		QueryRunner rmRunner = new RMRunner(index, stopper); // defaulting to 20 fbDocs and 20 fbTerms
		
		// Setup 10-fold cross validator, supplied with RMRunner
		KFoldValidator kfv = new KFoldValidator(rmRunner); // defaulting to 10-fold with 1000 results
		
		// Run the cross validation optimizing for MAP
		SearchHitsBatch batchResults = kfv.evaluate(queries, new MAPEvaluator(), qrels);

		// Write the results
		Iterator<String> qit = batchResults.queryIterator();
		while (qit.hasNext()) {
			String query = qit.next();
			output.write(batchResults.getSearchHits(query), query);			
		}
	}

}
