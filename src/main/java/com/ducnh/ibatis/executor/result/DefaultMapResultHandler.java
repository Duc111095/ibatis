package com.ducnh.ibatis.executor.result;

import java.util.Map;

import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.reflection.ReflectorFactory;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.reflection.wrapper.ObjectWrapperFactory;
import com.ducnh.ibatis.session.ResultContext;
import com.ducnh.ibatis.session.ResultHandler;

public class DefaultMapResultHandler<K, V> implements ResultHandler<V>{

	private final Map<K, V> mappedResults;
	private final String mapKey;
	private final ObjectFactory objectFactory;
	private final ObjectWrapperFactory objectWrapperFactory;
	private final ReflectorFactory reflectorFactory;
	
	@SuppressWarnings("unchecked")
	public DefaultMapResultHandler(String mapKey, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory,
		ReflectorFactory reflectorFactory) {
		this.objectFactory = objectFactory;
		this.objectWrapperFactory = objectWrapperFactory;
		this.reflectorFactory = reflectorFactory;
		this.mappedResults = objectFactory.create(Map.class);
		this.mapKey = mapKey;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void handleResult(ResultContext<? extends V> context) {
		final V value = context.getResultObject();
		final MetaObject mo = MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
		final K key = (K) mo.getValue(mapKey);
		mappedResults.put(key, value);
	}
}
