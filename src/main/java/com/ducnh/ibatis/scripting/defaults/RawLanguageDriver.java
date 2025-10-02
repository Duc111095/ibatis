package com.ducnh.ibatis.scripting.defaults;

import com.ducnh.ibatis.builder.BuilderException;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.parsing.XNode;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.scripting.xmltags.XMLLanguageDriver;
import com.ducnh.ibatis.session.Configuration;

public class RawLanguageDriver extends XMLLanguageDriver{

	@Override
	public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
		SqlSource source = super.createSqlSource(configuration, script, parameterType);
		checkIsNotDynamic(source);
		return source;
	}
	
	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		SqlSource source = super.createSqlSource(configuration, script, parameterType, paramNameResolver);
		checkIsNotDynamic(source);
		return source;
	}
	
	private void checkIsNotDynamic(SqlSource source) {
		if (!RawSqlSource.class.equals(source.getClass())) {
			throw new BuilderException("Dynamic content is not allowed when using RAW language");
		}
	}
}
