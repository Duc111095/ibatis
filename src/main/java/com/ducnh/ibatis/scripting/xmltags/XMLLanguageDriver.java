package com.ducnh.ibatis.scripting.xmltags;

import com.ducnh.ibatis.builder.xml.XMLMapperEntityResolver;
import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.parsing.PropertyParser;
import com.ducnh.ibatis.parsing.XNode;
import com.ducnh.ibatis.parsing.XPathParser;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.scripting.LanguageDriver;
import com.ducnh.ibatis.session.Configuration;

public class XMLLanguageDriver implements LanguageDriver{

	@Override
	public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject,
			BoundSql boundSql) {
		return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
	}

	@Override
	public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
		return createSqlSource(configuration, script, parameterType, null);
	}

	@Override
	public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType, paramNameResolver);
		return builder.parseScriptNode();
	}
	
	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
		return createSqlSource(configuration, script, parameterType, null);
	}
	
	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		if (script.startsWith("<script>")) {
			XPathParser parser = new XPathParser(script, false, configuration.getVariables(), new XMLMapperEntityResolver());
			return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
		}
		script = PropertyParser.parse(script, configuration.getVariables());
		TextSqlNode textSqlNode = new TextSqlNode(script);
		if (textSqlNode.isDynamic()) {
			return new DynamicSqlSource(configuration, textSqlNode);
		} else {
			return new RawSqlSource(configuration, script, parameterType, paramNameResolver);
		}
	}
}
