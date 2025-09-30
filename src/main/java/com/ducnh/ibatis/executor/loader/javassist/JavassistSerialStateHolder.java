package com.ducnh.ibatis.executor.loader.javassist;

import java.util.List;
import java.util.Map;

import com.ducnh.ibatis.executor.loader.AbstractSerialStateHolder;
import com.ducnh.ibatis.executor.loader.ResultLoaderMap;
import com.ducnh.ibatis.executor.loader.ResultLoaderMap.LoadPair;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;

class JavassistSerialStateHolder extends AbstractSerialStateHolder{

	private static final long serialVersionUID = 8940388717901644661L;
	
	public JavassistSerialStateHolder() {
	}
	
	public JavassistSerialStateHolder(final Object userBean,
		final Map<String, ResultLoaderMap.LoadPair> unloadedProperties, final ObjectFactory objectFactory,
		List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		super(userBean, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
	}
	
	@Override
	public Object createDeserializationProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
		ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		return new JavassistProxyFactory().createDeserializationProxy(target, unloadedProperties, objectFactory,
			constructorArgTypes, constructorArgs);
	}
}
