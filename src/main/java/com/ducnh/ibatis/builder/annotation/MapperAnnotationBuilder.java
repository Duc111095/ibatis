package com.ducnh.ibatis.builder.annotation;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.cert.PKIXRevocationChecker.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.ducnh.ibatis.annotations.Arg;
import com.ducnh.ibatis.annotations.CacheNamespace;
import com.ducnh.ibatis.annotations.CacheNamespaceRef;
import com.ducnh.ibatis.annotations.Case;
import com.ducnh.ibatis.annotations.Delete;
import com.ducnh.ibatis.annotations.DeleteProvider;
import com.ducnh.ibatis.annotations.Insert;
import com.ducnh.ibatis.annotations.InsertProvider;
import com.ducnh.ibatis.annotations.Options;
import com.ducnh.ibatis.annotations.Options.FlushCachePolicy;
import com.ducnh.ibatis.annotations.Property;
import com.ducnh.ibatis.annotations.Result;
import com.ducnh.ibatis.annotations.ResultMap;
import com.ducnh.ibatis.annotations.Results;
import com.ducnh.ibatis.annotations.Select;
import com.ducnh.ibatis.annotations.SelectKey;
import com.ducnh.ibatis.annotations.SelectProvider;
import com.ducnh.ibatis.annotations.TypeDiscriminator;
import com.ducnh.ibatis.annotations.Update;
import com.ducnh.ibatis.annotations.UpdateProvider;
import com.ducnh.ibatis.builder.BuilderException;
import com.ducnh.ibatis.builder.CacheRefResolver;
import com.ducnh.ibatis.builder.IncompleteElementException;
import com.ducnh.ibatis.builder.MapperBuilderAssistant;
import com.ducnh.ibatis.io.Resources;
import com.ducnh.ibatis.mapping.Discriminator;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.mapping.ResultSetType;
import com.ducnh.ibatis.mapping.SqlCommandType;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.mapping.StatementType;
import com.ducnh.ibatis.parsing.PropertyParser;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.UnknownTypeHandler;

public class MapperAnnotationBuilder {
	
	private static final Set<Class<? extends Annotation>> statementAnnotationTypes = Stream
		.of(Select.class, Update.class, Insert.class, Delete.class, SelectProvider.class, UpdateProvider.class,
			InsertProvider.class, DeleteProvider.class)
		.collect(Collectors.toSet());
	
	private final Configuration configuration;
	private final MapperBuilderAssistant assistant;
	private final Class<?> type;
	
	public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
		String resource = type.getName().replace('.', '/') + ".java (best guess)";
		this.assistant = new MapperBuilderAssistant(configuration, resource);
		this.configuration = configuration;
		this.type = type;
	}
	
	public void parse() {
		String resource = type.toString();
		if (!configuration.isResourceLoaded(resource)) {
			loadXmlResource();
			configuration.addLoadedResource(resource);
			assistant.setCurrentNamespace(type.getName());
			parseCache();
			parseCacheRef();
			for (Method method : type.getMethods()) {
				if (!canHaveStatement(method)) {
					continue;
				}
				if (getAnnotationWrapper(method, false, Select.class, SelectProvider.class).isPresent()
					&& method.getAnnotation(ResultMap.class) == null) {
					parseResultMap(method);
				}
				try {
					parseStatement(method);
				} catch (IncompleteElementException e) {
					configuration.addIncompleteMethod(new MethodResolver(this, method));
				}
			}
		}
		configuration.parsePendingMethods(false);
	}
	
	private static boolean canHaveStatement(Method method) {
		return !method.isBridge() && !method.isDefault();
	}
	
	private void loadXmlResource() {
		// Spring may not know the real resource name so we check a flag
		// to prevent loading again a resource twice
		// this flag is set at XMLMapperBuilder#bindMapperForNamespace
		if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
			String xmlResource = type.getName().replace('.', '/') + ".xml";
			InputStream inputStream = type.getResourceAsStream("/" + xmlResource);
			if (inputStream == null) {
				// Search XML mapper that is not in the module but in the classpath.
				try {
					inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
				} catch (Exception e2) {
					// ignore, resource is not required
				}
			}
			if (inputStream != null) {
				XMLMapperBuilder xmlParser = new XmlMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, 
					configuration.getSqlFragments(), type);
				xmlParser.parse();
			}
		}
	}
	
	private void parseCache() {
		CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
		if (cacheDomain != null) {
			Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
			Long flushInteval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
			Properties props = convertToProperties(cacheDomain.properties());
			assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInteval, size, 
					cacheDomain.readWrite(), cacheDomain.blocking(), props);
		}
	}
	
	private Properties convertToProperties(Property[] properties) {
		if (properties.length == 0) {
			return null;
		}
		Properties props = new Properties();
		for (Property property : properties) {
			props.setProperty(property.name(), PropertyParser.parse(property.value(), configuration.getVariables()));
		}
		return props;
	}
	
	private void parseCacheRef() {
		CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
		if (cacheDomainRef != null) {
			Class<?> refType = cacheDomainRef.value();
			String refName = cacheDomainRef.name();
			if (refType == void.class && refName.isEmpty()) {
				throw new BuilderException("Should be specified either value() or name() attribute in the @CacheNamespaceRef.");
			}
			if (refType != void.class && refName.isEmpty()) {
				throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");				
			}
			String namespace = refType != void.class ? refType.getName() : refName;
			try {
				assistant.useCacheRef(namespace);
			} catch (IncompleteElementException e) {
				configuration.addIncompleteCacheRef(new CacheRefResolver(assistant, namespace));
			}
		}
	}
	
	private String parseResultMap(Method method) {
		Class<?> returnType = getResultType(method, type);
		Arg[] args = method.getAnnotationsByType(Arg.class);
		Result[] results = method.getAnnotationsByType(Result.class);
		TypeDiscriminator typeDicriminator = method.getAnnotation(TypeDiscriminator.class);
		String resultMapId = generateResultMapName(method);
		applyResultMap(resultMapId, returnType, args, results, typeDicriminator);
		return resultMapId;
	}
	
	private String generateResultMapName(Method method) {
		Results results = method.getAnnotation(Results.class);
		if (results != null && !results.id().isEmpty()) {
			return type.getName() + "." + results.id();
		}
		StringBuilder suffix = new StringBuilder();
		for (Class<?> c : method.getParameterTypes()) {
			suffix.append("-");
			suffix.append(c.getSimpleName());
		}
		if (suffix.length() < 1) {
			suffix.append("-void");
		}
		return type.getName() + "." + method.getName() + suffix;
	}
	
	private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results,
		TypeDiscriminator discriminator) {
		List<ResultMapping> resultMappings = new ArrayList<>();
		applyConstructorArgs(args, returnType, resultMappings, resultMapId);
		applyResults(results, returnType, resultMappings);
		Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
		assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
		createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
	}
	
	private void createDisciminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
		if (discriminator != null) {
			for (Case c : discriminator.cases()) {
				String cassResultMapId = resultMapId + "-" + c.value();
				List<ResultMapping> resultMappings = new ArrayList<>();
				applyConstructorArgs(c.constructArgs(), resultType, resultMappings, resultMapId);
				applyResults(c.results(), resultType, resultMappings);
				assistant.addResultMap(cassResultMapId, c.type(), resultMapId, null, resultMappings, null);
			}
		}
	}
	
	private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
		if (discriminator != null) {
			String column = discriminator.column();
			Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
			JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
			
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (discriminator
				.typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
			Case[] cases = discriminator.cases();
			Map<String, String> discriminatorMap = new HashMap<>();
			for (Case c : cases) {
				String value = c.value();
				String caseResultMapId = resultMapId + "-" + value;
				discriminatorMap.put(value, caseResultMapId);
			}
			return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
		}
		return null;
	}
	
	void parseStatement(Method method) {
		final Class<?> parameterTypeClass = getParameterType(method);
		final ParamNameResolver paramNameResolver = new ParamNameResolver(configuration, method, type);
		final LanguageDriver languageDriver = getLanguageDriver(method);
		
		getAnnotationWrapper(method, true, statementAnnotationTypes).ifPresent(statementAnnotation -> {
			final SqlSource sqlSource = buildSqlSource(statementAnnotation.getAnnotation(), parameterTypeClass,
				paramNameResolver, languageDriver, method);
			final SqlCommandType sqlCommandType = statementAnnotation.getSqlCommandType();
			final Options options = getAnnotationWrapper(method, false, Option.class).map(x -> (Options) x.getAnnotation()).orElse(null);
			final String mappedStatementId = type.getName() + "." + method.getName();
			
			final KeyGenerator keyGenerator;
			String keyProperty = null;
			String keyColumn = null;
			if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
				// first check for SelectKey annotation - that overrides everything else
				SelectKey selectKey = getAnnotationWrapper(method, false, SelectKey.class)
					.map(x -> (SelectKey) x.getAnnotation()).orElse(null);
				if (selectKey != null) {
					keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method),
						paramNameResolver, languageDriver);
					keyProperty = selectKey.keyProperty();
				} else if (options == null) {
					keyGenerator = configuration.isUseGeneratedKey() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
				} else {
					keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
					keyProperty = options.keyProperty();
					keyColumn = options.keyColumn();
				}
			} else {
				keyGenerator = NoKeyGenerator.INSTANCE;
			}
			
			Integer fetchSize = null;
			Integer timeout = null;
			StatementType statementType = StatementType.PREPARED;
			ResutlSetType resultSetType = configuration.getDefaultResultSetType();
			boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
			boolean flushCache = !isSelect;
			boolean useCache = isSelect;
			if (options != null) {
				if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
					flushCache = true;
				} else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
					flushCache = false;
				}
				useCache = options.useCache();
				fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null;
				timeout = options.timeout() > -1 ? options.timeout() : null;
				statementType = options.statementType();
				if (options.resultSetType() != ResultSetType.DEFAULT) {
					resultSetType = options.resultSetType();
				}
			}
			
			String resultMapId = null;
			if (isSelect) {
				ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
				if (resultMapAnnotation != null) {
					resultMapId = String.join(",", resultMapAnnotation.value());
				} else {
					resultMapId = generateResultMapName(method);
				}
			}
			
			assistant.addMappedStatement(mappedStatementId, sqlSource, statementType, sqlCommandType, fetchSize, timeout, 
					null, parameterTypeClass, resultMapId, getReturnType(method, type), resultSetType, flushCache, useCache, 
					false, keyGenerator, keyProperty, keyColumn, statementAnnotation.getDatabaseId(), languageDriver, 
					options != null ? nullOrEmpty(options.resultSets()) : null, statementAnnotation.isDirtySelect()
					paramNameResolver);
		});
	}
	
	private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass, 
		ParamNameResolver paramNameResolver, LanguageDriver languageDriver) {
		return languageDriver.createSqlSource(configuration, String.join(" ", strings).trim(), parameterTypeClass
			, paramNameResolver);
				
	}
	
	private Optional<AnnotationWrapper> getAnnotationWrapper(Method method, boolean errorIfNoMatch,
		Class<? extends Annotation>... targetTypes) {
		return getAnnotationWrapper(method, errorIfNoMatch, Arrays.asList(targetTypes));
	}
	
	private Optional<AnnotationWrapper> getAnnotationWrapper(Method method, boolean errorIfNoMatch, 
		Collection<Class<? extends Annotation>> targetTypes) {
		String databaseId = configuration.getDatabaseId();
		Map<String, AnnotationWrapper> statementAnnotations = targetTypes.stream()
			.flatMap(x -> Arrays.stream(method.getAnnotationsByType(x))).map(AnnotationWrapper::new)
			.collect(Collectors.toMap(AnnotationWrapper::getDatabaseId, x -> x, (existing, duplicate) -> {
				throw new BuilderException(
					String.format("Detected conflicting annotations '%s' and '%s' on '%s'.", existing.getAnnotation(),
						duplicate.getAnnotation(), method.getDeclaringClass().getName() + "." + method.getName()));
			}));
		AnnotationWrapper annotationWrapper = null;
		if (databaseId != null) {
			annotationWrapper = statementAnnotations.get(databaseId);
		}
		if (annotationWrapper == null) {
			annotationWrapper = statementAnnotations.get("");
		}
		if (errorIfNoMatch && annotationWrapper == null && !statementAnnotations.isEmpty()) {
			// Annotations exist, but there is no matching one for the specified databaseId
			throw new BuilderException(String.format(
				"Could not find a statement annotation that correspond a current database or default statement on method '%s.%s'. Current database id is [%s].", 
					method.getDeclaringClass(), method.getName(), databaseId));
		}
		return Optional.ofNullable(annotationWrapper);
	}
	
	public static Class<?> getMethodReturnType(String mapperFqn, String localStatementId) {
		if (mapperFqn == null || localStatementId == null) {
			return null;
		}
		try {
			Class<?> mapperClass = Resources.classForName(mapperFqn);
			for (Method method : mapperClass.getMethods()) {
				if (method.getName().equals(localStatementId) && canHaveStatement(method)) {
					return getReturnType(method, mapperClass);
				}
			}
		} catch (ClassNotFoundException e) {
			// No corresponding mapper interface which is OK
		}
		return null;
	}
	
	private static class AnnotationWrapper {
		private final Annotation annotation;
		private final String databaseId;
		private final SqlCommandType sqlCommandType;
		private boolean dirtySelect;
		
		AnnotationWrapper(Annotation annotation) {
			this.annotation = annotation;
			if (annotation instanceof Select) {
				databaseId = ((Select) annotation).databaseId();
				sqlCommandType = SqlCommandType.SELECT;
				dirtySelect = ((Select) annotation).affectData();
			} else if (annotation instanceof Update) {
				databaseId = ((Update) annotation).databaseId();
				sqlCommandType = SqlCommandType.UPDATE;
			} else if (annotation instanceof Insert) {
				databaseId = ((Insert) annotation).databaseId();
				sqlCommandType = SqlCommandType.INSERT;
			} else if (annotation instanceof Delete) {
				databaseId = ((Delete) annotation).databaseId();
				sqlCommandType = SqlCommandType.DELETE;
			} else if (annotation instanceof SelectProvider) {
				databaseId = ((SelectProvider) annotation).databaseId();
				sqlCommandType = SqlCommandType.SELECT;
				dirtySelect = ((SelectProvider) annotation).affectData();
			} else if (annotation instanceof UpdateProvider) {
				databaseId = ((UpdateProvider) annotation).databaseId();
				sqlCommandType = SqlCommandType.UPDATE;
			} else if (annotation instanceof InsertProvider) {
				databaseId = ((InsertProvider) annotation).databaseId();
				sqlCommandType = SqlCommandType.INSERT;
			} else if (annotation instanceof DeleteProvider) {
				databaseId = ((DeleteProvider) annotation).databaseId();
				sqlCommandType = SqlCommandType.DELETE;
			} else {
				sqlCommandType = SqlCommandType.UNKNOWN;
				if (annotation instanceof Options) {
					databaseId = ((Options) annotation).databaseId();
				} else if (annotation instanceof SelectKey) {
					databaseId = ((SelectKey) annotation).databaseId();
				} else {
					databaseId = "";
				}
			}
 		}
		
		Annotation getAnnotation() {
			return annotation;
		}
		
		SqlCommandType getSqlCommandType() {
			return sqlCommandType;
		}
		
		String getDatabaseId() {
			return databaseId;
		}
		
		boolean isDirtySelect() {
			return dirtySelect;
		}
	}
}
