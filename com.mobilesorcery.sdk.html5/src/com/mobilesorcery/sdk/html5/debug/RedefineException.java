package com.mobilesorcery.sdk.html5.debug;

public class RedefineException extends Exception {

	public RedefineException(String message) {
		super(message);
	}

	public RedefineException(Exception cause) {
		super(cause);
	}

	public RedefineException(RedefinitionResult redefineResult) {
		this(redefineResult.getMessage());
	}

	public static RedefineException wrap(Exception e) {
		if (e instanceof RedefineException) {
			return (RedefineException) e;
		}
		return new RedefineException(e);
	}

}
