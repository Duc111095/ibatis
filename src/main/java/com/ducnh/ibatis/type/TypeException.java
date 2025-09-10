package com.ducnh.ibatis.type;

import com.ducnh.ibatis.exceptions.PersistenceException;

public class TypeException extends PersistenceException{
	
	private static final long serialVersionUID = 8614420898975117130L;
	
	public TypeException() {
		
	}
	
	public TypeException(String message) {
		super(message);
	}
	
	public TypeException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public TypeException(Throwable cause) {
		super(cause);
	}
}
