package com.ducnh.ibatis.executor.keygen;

import java.beans.Statement;

import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.mapping.MappedStatement;

public class NoKeyGenerator implements KeyGenerator{

	private static final NoKeyGenerator INSTANCE = new NoKeyGenerator();
	
	@Override
	public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		
	}
	
	@Override
	public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		
	}
}
