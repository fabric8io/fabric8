package org.fusesource.bai.policy.model;

import java.util.HashSet;

public class FilterSet<T extends Filter> extends HashSet<T> {

	private static final long serialVersionUID = 367911816167794989L;
	
	@SuppressWarnings("unchecked")
	public <A extends Filter> FilterSet<A> filtersOfType(Class<A> type) {
		FilterSet<A> answer = new FilterSet<A>();
		for (Filter f : this) {
			if (f.getClass().isAssignableFrom(type)) {
				answer.add((A) f);
			}
		}
		return answer;
	}
	
	public Filter firstMatch(Object o) {
		for (Filter f : this) {
			if (f.matches(o)) {
				return f;
			}
		}
		return null;
	}
	
	public FilterSet<T> allMatches(Object o) {
		FilterSet<T> answer = new FilterSet<T>();
		for (T f : this) {
			if (f.matches(o)) {
				answer.add(f);
			}
		}
		return answer;
	}

	public int countMatches(Object o) {
		int matches = 0;
		for (Filter f : this) {
			matches += f.matches(o) ? 1 : 0;
		}
		return matches;
	}
	
	public PolicySet returnPolicies() {
		PolicySet policies = new PolicySet();
		for (Filter filter : this) {
			policies.add(filter.getPolicy());
		}
		return policies;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("FilterSet [");
		for (Filter filter : this) {
			sb.append(filter.toString());
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length() -1);
		sb.append(']');
		return sb.toString();
	}
}
