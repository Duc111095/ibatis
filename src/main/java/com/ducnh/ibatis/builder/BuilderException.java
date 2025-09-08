package com.ducnh.ibatis.builder;

import com.ducnh.ibatis.exceptions.PersistenceException;

public class BuilderException extends PersistenceException{

	private static final long serialVersionUID = -3885164021020443281L;
	
	public BuilderException() {
		
	}
	
	public BuilderException(String message) {
		super(message);
	}
	
	public BuilderException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public BuilderException(Throwable cause) {
		super(cause);
	}
}
