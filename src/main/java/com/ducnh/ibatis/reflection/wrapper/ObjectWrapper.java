package com.ducnh.ibatis.reflection.wrapper;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.reflection.property.PropertyTokenizer;

public interface ObjectWrapper {
	Object get(PropertyTokenizer prop);
	
	void set(PropertyTokenizer prop, Object value);
	
	String findProperty(String name, boolean useCamelCaseMapping);
	
	String[] getGetterNames();
	
	String[] getSetterNames();
	
	Class<?> getSetterType(String name);
	
	Class<?> getGetterType(String name);
	
	default Entry<Type, Class<?>> getGenericSetterType(String name) {
		throw new UnsupportedOperationException(
			"'" + this.getClass() + "' must override the default method 'getGenericSetterType()'.");
	}
	
	default Entry<Type, Class<?>> getGenericGetterType(String name) {
		throw new UnsupportedOperationException(
			"'" + this.getClass() + "' must override the default method 'getGenericGetterType()'.");
	}
	
	boolean hasSetter(String name);
	
	boolean hasGetter(String name);
	
	MetaObject instantiaterPropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

	boolean isCollection();
	
	void add(Object element);
	
	<E> void addAll(List<E> element);
}
