package com.ducnh.ibatis.executor.keygen;


import java.sql.Statement;

import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.mapping.MappedStatement;

public interface KeyGenerator {

	void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);
	
	void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);
}
