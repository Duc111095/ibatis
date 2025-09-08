package com.ducnh.ibatis.reflection;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.reflection.property.PropertyTokenizer;
import com.ducnh.ibatis.reflection.wrapper.ObjectWrapper;
import com.ducnh.ibatis.reflection.wrapper.ObjectWrapperFactory;

public class MetaObject {
	
	private final Object originalObject;
	private final ObjectWrapper objectWrapper;
	private final ObjectFactory objectFactory;
	private final ObjectWrapperFactory objectWrapperFactory;
	private final ReflectorFactory reflectorFactory;
	
	private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory,
		ReflectorFactory reflectorFactory) {
		this.originalObject = object;
		this.objectFactory = objectFactory;
		this.objectWrapperFactory = objectWrapperFactory;
		this.reflectorFactory = reflectorFactory;
		
		if (object instanceof ObjectWrapper) {
			this.objectWrapper = (ObjectWrapper) object;
		} else if (objectWrapperFactory.hasWrapperFor(object)) {
			this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
		} else if (object instanceof Map) {
			this.objectWrapper = new MapMapper(this, (Map) object);
		} else if (object instanceof Collection) {
			this.objectWrapper = new CollectionMapper(this, (Collection) object);
		} else {
			this.objectWrapper = new BeanWrapper(this, object);
		}
	} 
	
	public static MetaObject forObject(Object object, ObjectFactory objectFactory,
		ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
		if (object == null) {
			return SystemMetaObject.NULL_META_OBJECT;
		}
		
		return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
	}
	
	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}
	
	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}
	
	public ReflectorFactory getReflectorFactory() {
		return reflectorFactory;
	}
	
	public Object getOriginalObject() {
		return originalObject;
	}
	
	public String findProperty(String propName, boolean useCamelCaseMapping) {
		return objectWrapper.findProperty(propName, useCamelCaseMapping);
	}
	
	public String[] getGetterNames() {
		return objectWrapper.getGetterNames();
	}
	
	public String[] getSetterNames() {
		return objectWrapper.getSetterNames();
	}
	
	public Class<?> getSetterType(String name) {
		return objectWrapper.getSetterType(name);
	}
	
	public Entry<Type, Class<?>> getGenericSetterType(String name) {
		return objectWrapper.getGenericSetterType(name);
	}
	
	public Class<?> getGetterType(String name) {
		return objectWrapper.getGetterType(name);
	}
	
	public Entry<Type, Class<?>> getGenericGetterType(String name) {
		return objectWrapper.getGenericGetterType(name);
	}
	
	public boolean hasSetter(String name) {
		return objectWrapper.hasSetter(name);
	}
	
	public boolean hasGetter(String name) {
		return objectWrapper.hasGetter(name);
	}
	
	public Object getValue(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		return objectWrapper.get(prop);
	}
	
	public void setValue(String name, Object value) {
		objectWrapper.set(new PropertyTokenizer(name), value);
	}
	
	public MetaObject metaObjectForProperty(String name) {
		Object value = getValue(name);
		return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
	}
	
	public ObjectWrapper getObjectWrapper() {
		return objectWrapper;
	}
	
	public boolean isCollection() {
		return objectWrapper.isCollection();
	}
	
	public void add(Object element) {
		objectWrapper.add(element);
	}
	
	public <E> void addAll(List<E> list) {
		objectWrapper.addAll(list);
	}
} 
