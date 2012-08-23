package org.fusesource.bai.model.policy;

import org.fusesource.bai.AuditEvent;
import org.fusesource.bai.model.policy.Constants.ActionType;

public class Action {

	/**
	 * Defines what type of action will execute when the policy matches (i.e. the policy is WITHIN {@link Scope}).
	 * For now, the actions are simple: 'INCLUDE' or 'EXCLUDE', in the sense that the {@link AuditEvent} will be included or excluded.
	 * In the future, we can extend this, and add more complex action definitions.
	 */
	public ActionType type;
	
}
