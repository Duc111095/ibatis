package com.ducnh.ibatis.exceptions;


public class IbatisException extends RuntimeException{
	
	private static final long serialVersionUID = 3880206998166270511L;
	
	public IbatisException() {
		
	}
	
	public IbatisException(String message) {
		super(message);
	}
	
	public IbatisException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public IbatisException(Throwable cause) {
		super(cause);
	}
}
