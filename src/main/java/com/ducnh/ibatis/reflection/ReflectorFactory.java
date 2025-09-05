package com.ducnh.ibatis.reflection;

import java.lang.reflect.Type;

public interface ReflectorFactory {
	
	boolean isClassCacheEnabled();
	
	void setClassCacheEnabled(boolean classCacheEnabled);
	
	Reflector findForClass(Type type);
}
