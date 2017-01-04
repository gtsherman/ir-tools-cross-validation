package edu.gslis.evaluation.running.runners.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gslis.queries.GQuery;

public class QueryParameters {

	private GQuery query;
	private int numResults;
	private Map<String, Double> params;
	
	private int hash;
	
	public QueryParameters(GQuery query, int numResults, Map<String, Double> params) {
		this.query = query;
		this.numResults = numResults;
		setParams(params);
	}
	
	/**
	 * Perform a deep copy of the params object since the values may be reassigned in the original
	 * @param params The params object to copy
	 */
	private void setParams(Map<String, Double> params) {
		this.params = new HashMap<String, Double>();
		for (String param : params.keySet()) {
			this.params.put(param, params.get(param));
		}
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
				// If param not in other, return false
				if (!k.getParams().containsKey(param)) {
					return false;
				}
				// If param value in other not equal to value here, return false
				if (!k.getParams().get(param).equals(params.get(param))) {
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
		if (hash == 0) {
			String s = new String();
			s += query.getTitle();
			s += numResults;
			
			List<Double> paramVals = new ArrayList<Double>(params.values());
			Collections.sort(paramVals);
			for (double p : paramVals) {
				s += p;
			}
			
			hash = s.hashCode();
		}
		return hash;
	}

}
