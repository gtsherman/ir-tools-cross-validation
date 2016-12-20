package edu.gslis.evaluation.running.runners.support;

import java.util.Map;

import edu.gslis.queries.GQuery;

public class QueryParameters {

	private GQuery query;
	private int numResults;
	private Map<String, Double> params;
	
	public QueryParameters(GQuery query, int numResults, Map<String, Double> params) {
		this.query = query;
		this.numResults = numResults;
		this.params = params;
	}
	
	public GQuery getQuery() {
		return query;
	}
	
	public int getNumResults() {
		return numResults;
	}

	public Map<String, Double> getParams() {
		return params;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof QueryParameters) {
			QueryParameters k = (QueryParameters) obj;

			for (String param : params.keySet()) {
				// If param not in other, or if param in other is not equal, return false
				if (!k.getParams().containsKey(param)
						|| !k.getParams().get(param).equals(params.get(param))) {
					return false;
				}
			}
			
			// If the numResults are not equal, return false
			if (k.getNumResults() != numResults) {
				return false;
			}

			// If we've reached here, all params are present and equal, 
			// so whether they are equal depends on whether the queries are equal.
			return query.equals(k.getQuery());
		}

		// The other object is either null or not a QueryParameters object
		return false;
	}
	
	@Override
	public int hashCode() {
		String s = new String();
		s += query.getTitle();
		s += numResults;
		for (double p : params.values()) {
			s += p;
		}
		return s.hashCode();
	}

}
