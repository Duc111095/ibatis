package com.ducnh.ibatis.mapping;

import java.util.List;

import com.ducnh.ibatis.session.Configuration;

public final class MappedStatement {
	
	private String resource;
	private Configuration configuration;
	private String id;
	private Integer fetchSize;
	private Integer timeout;
	private StatementType statementType;
	private ResultSetType resultSetType;
	private SqlSource sqlSource;
	private Cache cache;
	private ParameterMap parameterMap;
	private List<ResultMap> resultMaps;
	private boolean flushCacheRequired;
	private boolean useCache;
	private boolean resultOrdered;
	private SqlCommandType sqlCommandType;
	private KeyGenerator keyGenerator;
	private String[] keyProperties;
	private String[] keyColumns;
	private boolean hasNestedResultMaps;
	private String databaseId;
	private Log statementLog;
	private LanguageDriver lang;
	private String[] resultSets;
	private ParamNameResolver paramNameResolver;
	private boolean dirtySelect;
	
	MappedStatement() {
		
	}
	
	public static class Builder {
		private final MappedStatement mappedStatement = new MappedStatement();
		
		public Builder(Configuration configuration, String id, SqlSource sqlSource, SqlCommandType sqlCommandType) {
			mappedStatement.configuration = configuration;
			mappedStatement.id = id;
			mappedStatement.sqlSource = sqlSource;
			mappedStatement.statementType = StatementType.PREPARED;
			mappedStatement.resultSetType = ResultSetType.DEFAULT;
			
		}
	}
}
