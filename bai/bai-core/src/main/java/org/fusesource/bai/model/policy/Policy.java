package org.fusesource.bai.model.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * A Policy defines what should happen ({@link Action} when some set of conditions matches ({@link Scope}.
 * Conditions are defined by a set of Scope filters, that limit the application of the Action.
 * A condition could be: the event comes from a specific context, bundle, etc. or is of a specific type, etc.
 * @author Raul Kripalani
 */
public class Policy {

	/**
	 * One-to-many scope items that restrict the visibility of this {@link Policy}.
	 */
	public List<Scope> scope = new ArrayList<Scope>();
	
	/**
	 * One-to-one relationship with an action.
	 */
	public Action action = new Action();
	
}
