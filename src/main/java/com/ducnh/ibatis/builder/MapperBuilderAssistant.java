package com.ducnh.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.ducnh.ibatis.cache.Cache;
import com.ducnh.ibatis.cache.decorators.LruCache;
import com.ducnh.ibatis.cache.impl.PerpetualCache;
import com.ducnh.ibatis.executor.ErrorContext;
import com.ducnh.ibatis.mapping.CacheBuilder;
import com.ducnh.ibatis.mapping.Discriminator;
import com.ducnh.ibatis.mapping.ParameterMap;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.ParameterMode;
import com.ducnh.ibatis.mapping.ResultFlag;
import com.ducnh.ibatis.mapping.ResultMap;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeHandler;

public class MapperBuilderAssistant extends BaseBuilder{
	
	private String currentNamespace;
	private final String resource;
	private Cache currentCache;
	private boolean unresolvedCacheRef;
	
	public MapperBuilderAssistant(Configuration configuration, String resource) {
		super(configuration);
		ErrorContext.instance().resource(resource);
		this.resource = resource;
	}
	
	public String getCurrentNamespace() {
		return currentNamespace;
	}
	
	public void setCurrentNamespace(String currentNamespace) {
		if (currentNamespace == null) {
			throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
		}
		
		if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
			throw new BuilderException(
				"Wrong namespace. Expected '" + this.currentNamespace + "' but not found '" + currentNamespace + "'.");
		} 
		
		this.currentNamespace = currentNamespace;
	}
	
	public String applyCurrentNamespace(String base, boolean isReference) {
		if (base == null) {
			return null;
		}
		
		if (isReference) {
			// is it qualified with any namespace yet?
			if (base.contains(".")) {
				return base;
			}
		} else {
			if (base.startsWith(currentNamespace + ".")) {
				return base;
			}
			if (base.contains(".")) {
				throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
			}
		}
		return currentNamespace + "." + base;
	}
	
	public Cache useCacheRef(String namespace) {
		if (namespace == null) {
			throw new BuilderException("cache-ref element requires a namespace attribute.");
		}
		try {
			unresolvedCacheRef = true;
			Cache cache = configuration.getCache(namespace);
			if (cache == null) {
				throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
			}
			currentCache = cache;
			unresolvedCacheRef = false;
			return cache;
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("No cache for namespace '" + namespace +"' could be found.", e);
		}
	}
	
	public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval,
		Integer size, boolean readWrite, boolean blocking, Properties props) {
		Cache cache = new CacheBuilder(currentNamespace).implementation(valueOfDefault(typeClass, PerpetualCache.class))
				.addDecorator(valueOfDefault(evictionClass, LruCache.class)).clearInterval(flushInterval).size(size)
				.readWrite(readWrite).blocking(blocking).properties(props).build();
		configuration.addCache(cache);
		currentCache = cache;
		return cache;
	}
	
	public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
		id = applyCurrentNamespacE(id, false);
		ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
		configuration.addParameterMap(parameterMap);
		return parameterMap;
	}
	
	public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType,
		JdbcType jdbcType, String resultMap, ParameterMode parameterMode, Class<? extends TypeHandler<?>> typeHandler,
		Integer numericScale) {
		resultMap = applyCurrentNamespace(resultMap, true);
		Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, jdbcType, typeHandler);
		
		return new ParameterMapping.Builder(configuration, property, javaTypeClass).jdbcType(jdbcType)
			.resultMapId(resultMap).mode(parameterMode).numericScale(numericScale).typeHandler(typeHandlerInstance).build();
	}
	
	public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator,
		List<ResultMapping> resultMappings, Boolean autoMapping) {
		id = applyCurrentNamespace(id, false);
		extend = applyCurrentNamespace(extend, true);
		
		if (extend != null) {
			if (!configuration.hasResultMap(extend)) {
				throw new IncompleteElementException("Could not find a parent resultMap with id '" + extend + "'");
			}
			ResultMap resultMap = configuration.getResultMap(extend);
			List<ResultMapping> extendedResultMappings = new ArrayList<>(resultMap.getResultMappings());
			extendedResultMappings.removeAll(resultMappings);
			// Remove parent constructor if this resultMap declares a constructor.
			boolean declaresConstructor = false;
			for (ResultMapping resultMapping : resultMappings) {
				if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
					declaresConstructor = true;
					break;
				}
			}
			
			if (declaresConstructor) {
				extendedResultMappings.removeIf(resultMapping -> resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR));
			}
			resultMappings.addAll(extendedResultMappings);
		}
		ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping)
				.discriminator(discriminator).build();
		configuration.addResultMap(resultMap);
		return resultMap;
	}
	
}
 