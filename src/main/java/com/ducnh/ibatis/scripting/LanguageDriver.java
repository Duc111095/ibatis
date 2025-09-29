package com.ducnh.ibatis.scripting;

import java.lang.module.Configuration;

import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.parsing.XNode;
import com.ducnh.ibatis.reflection.ParamNameResolver;

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
	
	SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);
	
	default SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		return createSqlSource(configuration, script, parameterType);
	};
	
	SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);
	
	default SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		return createSqlSource(configuration, script, parameterType);
	}
}
