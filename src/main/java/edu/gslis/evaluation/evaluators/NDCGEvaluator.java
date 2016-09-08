package edu.gslis.evaluation.evaluators;

import java.util.Iterator;
import java.util.Set;

import edu.gslis.eval.Qrels;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.searchhits.SearchHitsBatch;

public class NDCGEvaluator implements Evaluator {
	
	private int rank;
	
	public NDCGEvaluator() {
		this(20);
	}
	
	public NDCGEvaluator(int rank) {
		setRankCutoff(rank);
	}
	
	public void setRankCutoff(int rank) {
		this.rank = rank;
	}

	public double evaluate(SearchHitsBatch batchResults, Qrels qrels) {
		return ndcg(rank, batchResults, qrels);
	}
	
	/**
	 * Compute nDCG@k and return average over all queries
	 * @param rankCutoff The k in nDCG@k
	 * @param batchResults Batch search results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double ndcg(int rankCutoff, SearchHitsBatch batchResults, Qrels qrels) {
		double avgNDCG = 0.0;

		Iterator<String> queryIt = batchResults.queryIterator();
		while (queryIt.hasNext()) {
			String query = queryIt.next();
			SearchHits results = batchResults.getSearchHits(query);

			double ndcg = ndcg(rankCutoff, query, results, qrels);
			avgNDCG += ndcg;
		}
		avgNDCG /= batchResults.getNumQueries();
		
		return avgNDCG;
	}
	
	/**
	 * Compute nDCG@k for a single query
	 * @param rankCutoff The k in nDCG@k
	 * @param query
	 * @param results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double ndcg(int rankCutoff, GQuery query, SearchHits results, Qrels qrels) {
		return ndcg(rankCutoff, query.getTitle(), results, qrels);
	}

	/**
	 * Compute nDCG@k for a single query
	 * @param rankCutoff The k in nDCG@k
	 * @param query
	 * @param results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double ndcg(int rankCutoff, String query, SearchHits results, Qrels qrels) {
		double dcg = dcg(rank, query, results, qrels);
		double idcg = idcg(rank, query, qrels);
		if (idcg == 0) {
			System.err.println("No relevant documents for query "+query+"?");
			return 0;
		}
		return dcg / idcg;
	}
	
	/**
	 * Compute DCG@k for a single query
	 * @param rankCutoff The k in DCG@k
	 * @param query
	 * @param results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double dcg(int rankCutoff, GQuery query, SearchHits results, Qrels qrels) {
		return dcg(rankCutoff, query.getTitle(), results, qrels);
	}
	
	/**
	 * Compute DCG@k for a single query
	 * @param rankCutoff The k in DCG@k
	 * @param query
	 * @param results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double dcg(int rankCutoff, String query, SearchHits results, Qrels qrels) {
		double dcg = 0.0;
		for (int i = 1; i <= rankCutoff; i++) {
			SearchHit hit = results.getHit(i-1);
			int rel = qrels.getRelLevel(query, hit.getDocno());
			dcg += dcgAtRank(i, rel);
		}
		return dcg;
	}
	
	/**
	 * Compute ideal DCG@k for a single query
	 * @param rankCutoff The k in DCG@k
	 * @param query
	 * @param results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double idcg(int rankCutoff, GQuery query, Qrels qrels) {
		return idcg(rankCutoff, query.getTitle(), qrels);
	}
	
	/**
	 * Compute ideal DCG@k for a single query
	 * @param rankCutoff The k in DCG@k
	 * @param query
	 * @param results
	 * @param qrels Relevance judgments
	 * @return
	 */
	public double idcg(int rankCutoff, String query, Qrels qrels) {
		SearchHits idealResults = new SearchHits();
		
		Set<String> relDocs = qrels.getRelDocs(query);

		if (relDocs == null) {
			return dcg(rankCutoff, query, idealResults, qrels);
		}

		for (String doc : relDocs) {
			int relLevel = qrels.getRelLevel(query, doc);

			SearchHit hit = new SearchHit();
			hit.setDocno(doc);
			hit.setScore(relLevel);
			idealResults.add(hit);
		}
		
		idealResults.rank();
		
		return dcg(rankCutoff, query, idealResults, qrels);
	}
	
	private double dcgAtRank(int rank, int rel) {
		return (double)(Math.pow(2, rel) - 1)/(Math.log(rank+1));
	}

}
