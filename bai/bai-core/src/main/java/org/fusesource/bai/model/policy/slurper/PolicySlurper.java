package org.fusesource.bai.model.policy.slurper;

import java.util.List;

import org.fusesource.bai.model.policy.Policy;

public interface PolicySlurper {

	public List<Policy> slurp(); 
	public List<Policy> refresh();
	public List<Policy> getPolicies();
	
}
