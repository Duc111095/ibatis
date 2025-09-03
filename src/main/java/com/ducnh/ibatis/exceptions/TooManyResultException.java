package com.ducnh.ibatis.exceptions;

public class TooManyResultException extends PersistenceException{
	
	private static final long serialVersionUID = 8935197089745865786L;
	
	public TooManyResultException() {
		
	}
	
	public TooManyResultException(String message) {
		super(message);
	}
	
	public TooManyResultException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TooManyResultException(Throwable cause) {
		super(cause);
	}
}
