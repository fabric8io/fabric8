package org.fusesource.bai.model.policy;

public class Constants {

	public enum ActionType {
		INCLUDE,
		EXCLUDE
	}
	
	public enum FilterElement {
		CONTEXT,
		EVENT,
		EXCHANGE,
		BUNDLE,
		ENDPOINT
	}
	
	public enum EventType {
		CREATED,
		COMPLETED,
		SENDING,
		SENT,
		FAILURE,
		FAILURE_HANDLED,
		REDELIVERY		
	}
	
	public enum FilterMethod {
		EXPRESSION,
		ENUM_VALUE_ONE,
		ENUM_VALUE_MULTIPLE
	}
	
}
