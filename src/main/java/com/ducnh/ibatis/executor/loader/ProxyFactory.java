package com.ducnh.ibatis.executor.loader;

import java.util.List;
import java.util.Properties;

import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.session.Configuration;

public interface ProxyFactory {
	
	default void setProperties(Properties properties) {
		
	}
	
	Object createProxy(Object target, ResultLoaderMap resultLoaderMap, Configuration configuration, 
		ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs);
}