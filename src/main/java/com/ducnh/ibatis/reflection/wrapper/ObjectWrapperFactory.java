package com.ducnh.ibatis.reflection.wrapper;

import com.ducnh.ibatis.reflection.MetaObject;

public interface ObjectWrapperFactory {
	boolean hasWrapperFor(Object object);
	
	ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);
}
