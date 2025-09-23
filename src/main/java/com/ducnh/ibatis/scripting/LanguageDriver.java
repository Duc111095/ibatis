package com.ducnh.ibatis.scripting;

import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;

public interface LanguageDriver {
	
	/**
	 * Create a ParameterHandler that passes the actual parameters to the JDBC statement.
	 * @param mappedStatement
	 * 		The mapped statement that is being executed
	 * @param parameterObject
	 * 		The input parameter object (can be null)
	 * @param boundSql
	 * 		The resulting SQL once the dynamic language has been executed
	 * 
	 * @return the parameter handler
	 */
	ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);
	
	
}
