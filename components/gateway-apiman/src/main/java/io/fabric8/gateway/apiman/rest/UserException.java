package io.fabric8.gateway.apiman.rest;

public class UserException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3505165281384216994L;

	public UserException() {
		super();
	}

	public UserException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UserException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserException(String message) {
		super(message);
	}

	public UserException(Throwable cause) {
		super(cause);
	}

}
