package com.ducnh.ibatis.reflection.wrapper;

import com.ducnh.ibatis.reflection.ReflectionException;

public class DefaultObjectWrapperFactory implements ObjectWrapperFactory{
	
	@Override
	public boolean hasWrapperFor(Object object) {
		return false;
	}
	
	@Override
	public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
		throw new ReflectionException(
			"The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");
	}
}
