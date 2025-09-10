package com.ducnh.ibatis.logging.nologging;

import com.ducnh.ibatis.logging.Log;

public class NoLoggingImpl implements Log{
	
	public NoLoggingImpl() {
		
	}
	
	@Override
	public boolean isDebugEnabled() {
		return false;
	}
	
	@Override
	public boolean isTraceEnabled() {
		return false;
	}
	
	@Override
	public void error(String s, Throwable e) {
		// Do nothing
	}
	
	@Override
	public void error(String s) {
		// Do nothing
	}
	
	@Override
	public void debug(String s) {
		// Do nothing
	}
	
	@Override
	public void trace(String s) {
		// Do nothing
	}
	
	@Override
	public void warn(String s) {
		// Do nothing
	}
}
