package com.ducnh.ibatis.builder.annotation;

import com.google.protobuf.Method;

public class ProviderContext {
	
	private final Class<?> mapperType;
	private final Method mapperMethod;
	private final String databaseId;
	
	ProviderContext(Class<?> mapperType, Method mapperMethod, String databaseId) {
		this.mapperMethod = mapperMethod;
		this.mapperType = mapperType;
		this.databaseId = databaseId;
	}
	
	public Class<?> getMapperType() {
		return mapperType;
	}
	
	public Method getMapperMethod() {
		return mapperMethod;
	}
	
	public String getDatabaseId() {
		return databaseId;
	}
}
