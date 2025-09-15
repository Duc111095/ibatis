package com.ducnh.ibatis.datasource;

import com.ducnh.ibatis.exceptions.PersistenceException;

public class DataSourceException extends PersistenceException {

	private static final long serialVersionUID = -‎5251396250407091334L‎;
	
	public DataSourceException() {
	}
	
	public DataSourceException(String message) {
		super(message);
	}
	
	public DataSourceException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DataSourceException(Throwable cause) {
		super(cause);
	}
}
