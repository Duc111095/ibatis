package com.ducnh.ibatis.scripting.xmltags;

import com.ducnh.ibatis.builder.SqlSourceBuilder;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.session.Configuration;

public class DynamicSqlSource implements SqlSource {

	private final Configuration configuration;
	private final SqlNode rootSqlNode;
	private final ParamNameResolver paramNameResolver;
	
	public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
		this(configuration, rootSqlNode, null);
	}
	
	public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode, ParamNameResolver paramNameResolver) {
		this.configuration = configuration;
		this.rootSqlNode = rootSqlNode;
		this.paramNameResolver = paramNameResolver;
	}
	
	@Override
	public BoundSql getBoundSql(Object parameterObject) {
		DynamicContext context = new DynamicContext(configuration, parameterObject, null, paramNameResolver, true);
		rootSqlNode.apply(context);
		String sql = context.getSql();
		SqlSource sqlSource = SqlSourceBuilder.buildSqlSource(configuration, sql, context.getParameterMappings());
		BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
		context.getBindings().forEach(boundSql::setAdditionalParameter);
		return boundSql;
	}
}
