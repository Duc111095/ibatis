package com.ducnh.ibatis.exceptions;

import com.ducnh.ibatis.executor.ErrorContext;

public class ExceptionFactory {
	
	private ExceptionFactory() {
		
	}
	
	public static RuntimeException wrapException(String message, Exception e) {
		return new PersistenceException(ErrorContext.instance().message(message).cause(e).toString(), e);
	}
}
