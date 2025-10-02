package com.ducnh.ibatis.scripting.defaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ducnh.ibatis.builder.ParameterMappingTokenHandler;
import com.ducnh.ibatis.builder.SqlSourceBuilder;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.parsing.GenericTokenParser;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.scripting.xmltags.DynamicContext;
import com.ducnh.ibatis.scripting.xmltags.SqlNode;
import com.ducnh.ibatis.session.Configuration;

public class RawSqlSource implements SqlSource{

	private final SqlSource sqlSource;
	
	public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
		this(configuration, rootSqlNode, parameterType, null);
	}
	
	public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType, 
		ParamNameResolver paramNameResolver) {
		DynamicContext context = new DynamicContext(configuration, parameterType, paramNameResolver);
		rootSqlNode.apply(context);
		String sql = context.getSql();
		sqlSource = SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
	}
	
	public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
		this(configuration, sql, parameterType, null);
	}
	
	public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		Class<?> clazz = parameterType == null ? Object.class : parameterType;
		List<ParameterMapping> parameterMappings = new ArrayList<>();
		ParameterMappingTokenHandler tokenHandler = new ParameterMappingTokenHandler(parameterMappings, configuration, 
			clazz, new HashMap<>(), paramNameResolver);
		GenericTokenParser parser = new GenericTokenParser("#{", "}", tokenHandler);
		sqlSource = SqlSourceBuilder.buildSqlSource(configuration, parser.parse(sql), parameterMappings);
	}
	
	@Override
	public BoundSql getBoundSql(Object parameterObject) {
		return sqlSource.getBoundSql(parameterObject);
	}
}
