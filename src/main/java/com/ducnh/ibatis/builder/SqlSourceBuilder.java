package com.ducnh.ibatis.builder;

import java.util.List;
import java.util.StringTokenizer;

import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.session.Configuration;

public class SqlSourceBuilder {
	
	private SqlSourceBuilder() {
		super();
	}
	
	public static SqlSource buildSqlSource(Configuration configuration, String sql, 
		List<ParameterMapping> parameterMappings) {
		return new StaticSqlSource(configuration, 
			configuration.isShrinkWhitespacesInSql() ? SqlSourceBuilder.removeExtraWhitespaces(sql) : sql);
	}
	
	public static String removeExtraWhitespaces(String original) {
		StringTokenizer tokenizer = new StringTokenizer(original);
		StringBuilder builder = new StringBuilder();
		boolean hasMoreTokens = tokenizer.hasMoreTokens();
		while (hasMoreTokens) {
			builder.append(tokenizer.nextToken());
			hasMoreTokens = tokenizer.hasMoreTokens();
			if (hasMoreTokens) {
				builder.append(' ');
			}
		}
		return builder.toString();
	}
}
