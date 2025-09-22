package com.ducnh.ibatis.reflection;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultReflectorFactory implements ReflectorFactory{
	private boolean classCacheEnabled = true;
	private final ConcurrentMap<Type, Reflector> reflectorMap = new ConcurrentHashMap<>();
	
	public DefaultReflectorFactory() {
		
	}
	
	@Override
	public boolean isClassCacheEnabled() {
		return classCacheEnabled;
	}
	
	@Override
	public void setClassCacheEnabled(boolean classCacheEnabled) {
		this.classCacheEnabled = classCacheEnabled;
	}
	
	@Override
	public Reflector findForClass(Type type) {
		if (classCacheEnabled) {
			return reflectorMap.computeIfAbsent(type, Reflector::new);
		}
		return new Reflector(type);
	}
}
