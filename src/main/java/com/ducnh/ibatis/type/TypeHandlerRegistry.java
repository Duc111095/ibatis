package com.ducnh.ibatis.type;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.ducnh.ibatis.session.Configuration;

public final class TypeHandlerRegistry {
	
	private final Map<JdbcType, TypeHandler<?>> jdbcTypeHandlerMap = new EnumMap<>(JdbcType.class);
	private final Map<Type, Map<JdbcType, TypeHandler<?>>> typeHandlerMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Type, Constructor<?>> smartHandlers = new ConcurrentHashMap<>();
	private final Map<Class<?>, TypeHandler<?>> allTypeHandlersMap = new HashMap<>();
	
	private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = Collections.emptyMap();
	
	@SuppressWarnings("rawtypes")
	private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class; 
	
	public TypeHandlerRegistry() {
		this(new Configuration());
	}
	
	public TypeHandlerRegistry(Configuration configuration) {
		register(new Type[] {Boolean.class, boolean.class}, new JdbcType[] {null}, BooleanTypeHandler.class);
		
		jdbcTypeHandlerMap.put(JdbcType.BOOLEAN, BooleanTypeHandler.INSTANCE);
	}
	
	public void setDefaultEnumTypeHandler(@SuppressWarnings("rawtypes") Class<? extends TypeHandler> typeHandler) {
		this.defaultEnumTypeHandler = typeHandler;
	}
	
	public boolean hasTypeHandler(Type javaType) {
		return hasTypeHandler(javaType, null);
	}
	
	public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
		return hasTypeHandler(javaTypeReference, null);
	}
	
	public boolean hasTypeHandler(Type javaType, JdbcType jdbcType) {
		return javaType != null && getTypeHandler(javaType, jdbcType) != null;
	}
	
	public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
		return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
	}
	
	public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
		return allTypeHandlersMap.get(handlerType);
	}
	
	public TypeHandler<?> getTypeHandler(Type type) {
		return getTypeHandler(type, null);
	}
	
	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
		return getTypeHandler(javaTypeReference, null);
	}
	
	public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
		return jdbcTypeHandlerMap.get(jdbcType);
	}
	
	@SuppressWarnings("unchecked")
	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
		return (TypeHandler<T>) getTypeHandler(javaTypeReference.getRawType(), jdbcType);
	}
	
	public TypeHandler<?> getTypeHandler(Type type, JdbcType jdbcType, Class<? extends TypeHandler<?>> typeHandlerClass) {
		TypeHandler<?> typeHandler = getTypeHandler(type, jdbcType);
		if (typeHandler != null && (typeHandlerClass == null || typeHandler.getClass().equals(typeHandlerClass))) {
			return typeHandler;
		}
		
		if (typeHandlerClass == null) {
			typeHandler = getSmartHandler(type, jdbcType);
		} else {
			typeHandler = getMappingTypeHandler(typeHandlerClass);
			if (typeHandler == null) {
				typeHandler = getInstance(type, typeHandlerClass);
			}
		}
		return typeHandler;
	}
	
	public TypeHandler<?> getTypeHandler(Type type, JdbcType jdbcType) {
		if (ParamMap.class.equals(type)) {
			return null;
		} else if (type == null) {
			return getTypeHandler(jdbcType);
		}
		
		TypeHandler<?> handler = null;
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
		
		if (Object.class.equals(type)) {
			if (jdbcHandlerMap != null) {
				handler = jdbcHandlerMap.get(jdbcType);
			}
			return handler;
		}
		
		if (jdbcHandlerMap != null) {
			handler = jdbcHandlerMap.get(jdbcType);
			if (handler == null) {
				handler = jdbcHandlerMap.get(null);
			}
			if (handler == null) {
				handler = pickSoleHandler(jdbcHandlerMap);
			}
		}
		if (handler == null) {
			handler = getSmartHandler(type, jdbcType);
		}
		
		if (handler == null && type instanceof ParameterizedType) {
			handler = getTypeHandler((Class<?>) ((ParameterizedType) type).getRawType(), jdbcType);
		}
		return handler;
	}
	
	private TypeHandler<?> getSmartHandler(Type type, JdbcType jdbcType) {
		Constructor<?> candidate = null;
		
		for (Entry<Type, Constructor<?>> entry : smartHandlers.entrySet()) {
			Type registeredType = entry.getKey();
			if (registeredType == type) {
				candidate = entry.getValue();
				break;
			}
			if (registeredType instanceof Class) {
				if (type instanceof Class && ((Class<?>) registeredType).isAssignableFrom((Class<?>) type)) {
					candidate = entry.getValue();
				}
			} else if (registeredType instanceof ParameterizedType) {
				Class<?> registeredClass = (Class<?>) ((ParameterizedType) registeredType).getRawType();
				if (type instanceof ParameterizedType) {
					Class<?> clazz = (Class<?>) ((ParameterizedType) registeredType).getRawType();
					if (registeredClass.isAssignableFrom(clazz)) {
						candidate = entry.getValue();
					}
				}
			}
		}
		
		if (candidate == null) {
			if (type instanceof Class) {
				Class<?> clazz = (Class<?>) type;
				if (Enum.class.isAssignableFrom(clazz)) {
					Class<?> enumClass = clazz.isAnonymousClass() ? clazz.getSuperclass() : clazz;
					TypeHandler<?> enumHandler = getInstance(enumClass, defaultEnumTypeHandler);
					register(new Type[] {enumClass}, new JdbcType[] {jdbcType}, enumHandler);
					return enumHandler;
				}
			}
			return null;
		}
		
		try {
			TypeHandler<?> typeHandler = (TypeHandler<?>) candidate.newInstance(type);
			register(type, jdbcType, typeHandler);
			return typeHandler;
		} catch (ReflectiveOperationException e) {
			throw new TypeException("Failed to invoke constructor " + candidate.toString(), e);
		}
	}
	
	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(type);
		if (jdbcHandlerMap != null) {
			return NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap) ? null : jdbcHandlerMap;
		}
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			if (!Enum.class.isAssignableFrom(clazz)) {
				jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
			}
		}
		typeHandlerMap.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
		return jdbcHandlerMap;
	}
	
	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
		Class<?> superClass = clazz.getSuperclass();
		if (superClass == null || Object.class.equals(superClass)) {
			return null;
		}
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = typeHandlerMap.get(superClass);
		if (jdbcHandlerMap != null) {
			return jdbcHandlerMap;
		}
		
		return getJdbcHandlerMapForSuperclass(superClass);
	}
	
	private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
		TypeHandler<?> soleHandler = null;
		for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
			if (soleHandler == null) {
				soleHandler = handler;
			} else if (!handler.getClass().equals(soleHandler.getClass())) {
				return null;
			}
		}
		return soleHandler;
	}
	
	public void register(JdbcType mappedJdbcType, TypeHandler<?> handler) {
		jdbcTypeHandlerMap.put(mappedJdbcType, handler);
	}
	
	public <T> void register(TypeHandler<T> handler) {
		register(mapperJavaTypes(handler.getClass()), mappedJdbcTypes(handler.getClass()), handler);
	}
	
	public void register(Class<?> mappedJavaType, TypeHandler<?> handler) {
		register((Type) mappedJavaType, handler);
	}
	
	private void register(Type mappedJavaType, TypeHandler<?> handler) {
		register(new Type[] {mappedJavaType}, mappedJdbcTypes(handler.getClass()), handler);
	}
	
	public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
		register(javaTypeReference.getRawType(), handler);
	}
	
	public void register(Type mappedJavaType, JdbcType mappedJdbcType, TypeHandler<?> handler) {
		register(new Type[] {mappedJavaType}, new JdbcType[] {mappedJdbcType}, handler);
	}
	
	
}
