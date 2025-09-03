package com.ducnh.ibatis.logging;

import com.ducnh.ibatis.exceptions.PersistenceException;

public class LogException extends PersistenceException {
	
	private static final long serialVersionUID = 1022924004852350942L;
	
	public LogException() {
		
	}
	
	public LogException(String message) {
		super(message);
	}
	
	public LogException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public LogException(Throwable cause) {
		super(cause);
	}
}
