package com.ducnh.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ducnh.ibatis.builder.BaseBuilder;
import com.ducnh.ibatis.builder.BuilderException;
import com.ducnh.ibatis.builder.MapperBuilderAssistant;
import com.ducnh.ibatis.parsing.XNode;
import com.ducnh.ibatis.parsing.XPathParser;
import com.ducnh.ibatis.session.Configuration;

public class XMLMapperBuilder extends BaseBuilder{

	private final XPathParser parser;
	private final MapperBuilderAssistant builderAssistant;
	private final Map<String, XNode> sqlFragments;
	private final String resource;
	private Class<?> mapperClass;
	
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments,
		String namespace) {
		this(reader, configuration, resource, sqlFragments);
		this.builderAssistant.setCurrentNamespace(namespace);
	}
	
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, 
		Map<String, XNode> sqlFragments) {
		this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration,
			resource, sqlFragments);
	}
	
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource,
		Map<String, XNode> sqlFragments, Class<?> mapperClass) {
		this(inputStream, configuration, resource, sqlFragments, mapperClass.getName());
		this.mapperClass = mapperClass;
	}
	
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, 
		Map<String, XNode> sqlFragments, String namespace) {
		this(inputStream, configuration, resource, sqlFragments);
		this.builderAssistant.setCurrentNamespace(namespace);
	}
	
	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, 
		Map<String, XNode> sqlFragments) {
		this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration,
			resource, sqlFragments);
	}
	
	public XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, 
		Map<String, XNode> sqlFragments) {
		super(configuration);
		this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
		this.parser = parser;
		this.sqlFragments = sqlFragments;
		this.resource = resource;
	} 
	
	public void parse() {
		if (!configuration.isResourceLoader(resource)) {
			configurationElement(parser.evalNode("/mapper"));
			configuration.addLoadedResource(resource);
			bindMapperForNamespace();
		}
		configuration.parsePendingResultMaps(false);
		configuration.parsePendingCacheRefs(false);
		configuration.parsePendingStatements(false);
	}
	
	public XNode getSqlFragment(String refid) {
		return sqlFragments.get(refid);
	}
	
	private void configurationElement(XNode context) {
		try {
			String namespace = context.getStringAttribute("namespace");
			if (namespace == null || namespace.isEmpty()) {
				throw new BuilderException("Mapper's namespace cannot be empty");
			}
			builderAssistant.setCurrentNamespace(namespace);
			cacheRefElement(context.evalNode("cache-ref"));
			cacheElement(context.evalNode("cache"));
			paramterMapElement(context.evalNodes("/mapper/parameterMap"));
			resultMapElements(context.evalNodes("/mapper/resultMap"));
			sqlElement(context.evalNodes("/mapper/sql"));
			buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
		}
	}
	
	private void buildStatementFromContext(List<XNode> list) {
		if (configuration.getDatabaseId() != null) {
			buildStatementFromContext(list, configuration.getDatabaseId());
		} 
		buildStatementFromContext(list, null);
	}
	
	private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
		for (XNode context : list) {
			final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, 
				requiredDatabaseId, mapperClass);
			try {
				
			}
		}
	}
}
