package com.ducnh.ibatis.builder;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.ducnh.ibatis.mapping.ParameterMode;
import com.ducnh.ibatis.mapping.ResultSetType;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeAliasRegistry;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.TypeHandlerRegistry;

public abstract class BaseBuilder {
	protected final Configuration configuration;
	protected final TypeAliasRegistry typeAliasRegistry;
	protected final TypeHandlerRegistry typeHandlerRegistry;

	public BaseBuilder(Configuration configuration) {
		this.configuration = configuration;
		this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
		this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
	}
	
	public Configuration getConfiguration() {
		return configuration;
	}
	
	protected Pattern parseExpression(String regex, String defaultValue) {
		return Pattern.compile(regex == null ? defaultValue : regex);
	}
	
	protected Boolean booleanValueOf(String value, Boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf(value);
	}
	
	protected Integer integerValueOf(String value, Integer defaultValue) {
		return value == null ? defaultValue : Integer.valueOf(value);
	}
	
	protected Set<String> stringSetValueOf(String value, String defaultValue) {
		value = value == null ? defaultValue : value;
		return new HashSet<>(Arrays.asList(value.split(",")));
	}
	
	protected JdbcType resolveJdbcType(String alias) {
		try {
			return alias == null ? null : JdbcType.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
		}
	}
	
	protected ResultSetType resolveResultSetType(String alias) {
		try {
			return alias == null ? null : ResultSetType.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
		}
	}
	
	protected ParameterMode resolveParameterMode(String alias) {
		try {
			return alias == null ? null : ParameterMode.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
		}
	}
	
	protected Object createInstance(String alias) {
		Class<?> clazz = resolveClass(alias);
		try {
			return clazz == null ? null : clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new BuilderException("Error creating instance. Cause: " + e, e);
		}
	}
	
	protected <T> Class<? extends T> resolveClass(String alias) {
		try {
			return alias == null ? null : resolveAlias(alias);
		} catch (Exception e) {
			throw new BuilderException("Error resolving claas. Cause: " + e, e);
		}
	}
	
	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
		return resolveTypeHandler(null, javaType, null, typeHandlerAlias);
	}
	
	protected TypeHandler<?> resolveTypeHandler(Class<?> parameterType, Type propertyType, JdbcType jdbcType,
		String typeHandlerAlias) {
		Class<? extends TypeHandler<?>> typeHandlerType = null;
		typeHandlerType = resolveClass(typeHandlerAlias);
		if (typeHandlerType != null && !TypeHandler.class.isAssignableFrom(typeHandlerType)) {
			throw new BuilderException("Type " + typeHandlerType.getName() 
				+ " is not a valid TypeHandler because it does not implement TypeHandler interface"); 
		}
		return resolveTypeHandler(propertyType, jdbcType, typeHandlerType);
	}
	
	protected TypeHandler<?> resolveTypeHandler(Type javaType, JdbcType jdbcType,
		Class<? extends TypeHandler<?>> typeHandlerType) {
		if (typeHandlerType == null && jdbcType == null) {
			return null;
		}
		return configuration.getTypeHandlerRegistry().getTypeHandler(javaType, jdbcType, typeHandlerType);
	}
	
	protected <T> Class<? extends T> resolveAlias(String alias) {
		return typeAliasRegistry.resolveAlias(alias);
	}
}
