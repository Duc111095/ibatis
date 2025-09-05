package com.ducnh.ibatis.reflection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.ducnh.ibatis.reflection.invoker.Invoker;
import com.ducnh.ibatis.reflection.property.PropertyTokenizer;

public class MetaClass {
	private final ReflectorFactory reflectorFactory;
	private final Reflector reflector;
	
	private MetaClass(Type type, ReflectorFactory reflectorFactory) {
		this.reflectorFactory = reflectorFactory;
		this.reflector = reflectorFactory.findForClass(type);
	}
	
	public static MetaClass forClass(Type type, ReflectorFactory reflectorFactory) {
		return new MetaClass(type, reflectorFactory);
	}
	
	public MetaClass metaClassForProperty(String name) {
		Class<?> propType = reflector.getGetterType(name);
		return MetaClass.forClass(propType, reflectorFactory);
	}
	
	public String findProperty(String name) {
		StringBuilder prop = buildProperty(name, new StringBuilder());
		return prop.length() > 0 ? prop.toString() : null;
	}
	
	public String findProperty(String name, boolean useCamelCaseMapping) {
		if (useCamelCaseMapping) {
			name = name.replace("_", "");
		}
		return findProperty(name);
	}
	
	public String[] getGetterNames() {
		return reflector.getGetablePropertyNames();
	}
	
	public String[] getSetterNames() {
		return reflector.getSetablePropertyNames();
	}
	
	public Class<?> getSetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop.getName());
			return metaProp.getSetterType(prop.getChildren());
		}
		return reflector.getSetterType(prop.getName());
	}
	
	public Entry<Type, Class<?>> getGenericSetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop);
			return metaProp.getGenericSetterType(prop.getChildren());
		}
		return reflector.getGenericGetterType(prop.getName());
	}
	
	public Class<?> getGetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop);
			return metaProp.getGetterType(prop.getChildren());
		}
		return getGetterType(prop).getValue();
	}
	
	public Entry<Type, Class<?>> getGenericGetterType(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			MetaClass metaProp = metaClassForProperty(prop);
			return metaProp.getGenericGetterType(prop.getChildren());
		}
		return getGetterType(prop);
	}
	
	private MetaClass metaClassForProperty(PropertyTokenizer prop) {
		Class<?> propType = getGetterType(prop).getValue();
		return MetaClass.forClass(propType, reflectorFactory);
	}
	
	private Entry<Type, Class<?>> getGetterType(PropertyTokenizer prop) {
		Entry<Type, Class<?>> pair = reflector.getGenericGetterType(prop.getName());
		if (prop.getIndex() != null && Collection.class.isAssignableFrom(pair.getValue())) {
			Type returnType = pair.getKey();
			if (returnType instanceof ParameterizedType) {
				Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
				if (actualTypeArguments != null && actualTypeArguments.length == 1) {
					returnType = actualTypeArguments[0];
					if (returnType instanceof Class) {
						return Map.entry(returnType, (Class<?>) returnType);
					} else if (returnType instanceof ParameterizedType) {
						return Map.entry(returnType, (Class<?>) ((ParameterizedType) returnType).getRawType());
					}
				}
 			}
		}
		return pair;
	}
	
	public boolean hasSetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (!prop.hasNext()) {
			return reflector.hasSetter(prop.getName());
		}
		if (reflector.hasSetter(prop.getName())) {
			MetaClass metaProp = metaClassForProperty(prop.getName());
			return metaProp.hasSetter(prop.getChildren());
		}
		
		return false;
	}
	
	public boolean hasGetter(String name) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (!prop.hasNext()) { 
			return reflector.hasGetter(prop.getName());
		}
		if (reflector.hasGetter(prop.getName())) {
			MetaClass metaProp = metaClassForProperty(prop.getName());
			return metaProp.hasGetter(prop.getChildren());
		}
		
		return false;
	}
	
	public Invoker getGetInvoker(String name) {
		return reflector.getGetInvoker(name);
	}
	
	public Invoker getSetInvoker(String name) {
		return reflector.getSetInvoker(name);
	}
	
	private StringBuilder buildProperty(String name, StringBuilder builder) {
		PropertyTokenizer prop = new PropertyTokenizer(name);
		if (prop.hasNext()) {
			String propertyName = reflector.findPropertyName(prop.getName());
			if (propertyName != null) {
				builder.append(propertyName);
				builder.append(".");
				MetaClass metaProp = metaClassForProperty(propertyName);
				metaProp.buildProperty(prop.getChildren(), builder);
			}
		} else {
			String propertyName = reflector.findPropertyName(name);
			if (propertyName != null) {
				builder.append(propertyName);
			}
		}
		return builder;
	}
	
	public boolean hasDefaultConstructor() {
		return reflector.hasDefaultConstructor();
	}
}
 