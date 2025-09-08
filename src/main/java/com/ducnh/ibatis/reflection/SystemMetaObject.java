package com.ducnh.ibatis.reflection;

import com.ducnh.ibatis.reflection.factory.DefaultObjectFactory;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import com.ducnh.ibatis.reflection.wrapper.ObjectWrapperFactory;


public final class SystemMetaObject {
	public static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	public static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	public static final MetaObject NULL_META_OBJECT = MetaObject.forObject(new NullObject(), DEFAULT_OBJECT_FACTORY, 
			DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
	
	private SystemMetaObject() {
		
	}
	
	private static class NullObject {
	}
	
	public static MetaObject forObject(Object object) {
		return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY, 
			new DefaultReflectorFactory());
	}
	
}
