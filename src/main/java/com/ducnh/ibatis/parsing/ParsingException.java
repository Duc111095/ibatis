package com.ducnh.ibatis.parsing;

import com.ducnh.ibatis.exceptions.PersistenceException;

public class ParsingException extends PersistenceException{
	
	private static final long serialVersionUID = -176685891441325943L;
	
	public ParsingException() {
		
	}
	
	public ParsingException(String message) {
		super(message);
	}
	
	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ParsingException(Throwable cause) {
		super(cause);
	}
}
