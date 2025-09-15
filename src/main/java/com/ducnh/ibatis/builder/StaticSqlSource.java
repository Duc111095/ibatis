package com.ducnh.ibatis.builder;

import java.util.List;

import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.session.Configuration;

public class StaticSqlSource implements SqlSource{
	
	private final String sql;
	private final List<ParameterMapping> parameterMappings;
	private final Configuration configuration;

	public StaticSqlSource(Configuration configuration, String sql) {
		this(configuration, sql, null);
	}
	
	public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
		this.sql = sql;
		this.parameterMappings = parameterMappings;
		this.configuration = configuration;
	}
	
	@Override
	public BoundSql getBoundSql(Object parameterObject) {
		return new BoundSql(configuration, sql, parameterMappings, parameterObject);
	}
}
