package com.ducnh.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ducnh.ibatis.annotations.Delete.List;
import com.ducnh.ibatis.annotations.Param;
import com.ducnh.ibatis.reflection.property.PropertyTokenizer;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;

public class ParamNameResolver {
	
	public static final String GENERIC_NAME_PREFIX = "param";
	
	public static final String[] GENERIC_NAME_CACHE = new String[10];
	
	static {
		for (int i = 0; i < 10; i++) {
			GENERIC_NAME_CACHE[i] = GENERIC_NAME_PREFIX + (i + 1);
		}
	}
	
	private final boolean useActualParamName;
	
	private final SortedMap<Integer, String> names;
	private final Map<String, Type> typeMap = new HashMap<>();
	private boolean hasParamAnnotation;
	private boolean useParamMap;
	
	public ParamNameResolver(Configuration configuration, Method method, Class<?> mappedClass) {
		this.useActualParamName = configuration.isUseActualParamName();
		final Class<?>[] paramTypes = method.getParameterTypes();
		final Annotation[][] paramAnnotations = method.getParameterAnnotations();
		final SortedMap<Integer, String> map = new TreeMap<>();
		Type[] actualParamTypes = TypeParameterResolver.resolveParamTypes(method, mappedClass);
		int paramCount = paramAnnotations.length;
		// get names from @Param annotations
		for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
			if (isSpecialParameter(paramTypes[paramIndex])) {
				// skip special parameters
				continue;
			}
			String name = null;
			for (Annotation annotation : paramAnnotations[paramIndex]) {
				if (annotation instanceof Param) {
					hasParamAnnotation = true;
					useParamMap = true;
					name = ((Param) annotation).value();
					break;
				}
			}
			if (name == null) {
				if (useActualParamName) {
					name = getActualParamName(method, paramIndex);
				}
				if (name == null) {
					name = String.valueOf(map.size());
				}
			}
			map.put(paramIndex, name);
			typeMap.put(name, actualParamTypes[paramIndex]);
		}
		names = Collections.unmodifiableSortedMap(map);
		if (names.size() > 1) {
			useParamMap = true;
		}
		if (names.size() == 1) {
			Type soleParamType = actualParamTypes[0];
			if (soleParamType instanceof GenericArrayType) {
				typeMap.put("array", soleParamType);
			} else {
				Class<?> soleParamClass = null;
				if (soleParamType instanceof ParameterizedType) {
					soleParamClass = (Class<?>)((ParameterizedType) soleParamType).getRawType();
				} else if (soleParamType instanceof Class) {
					soleParamClass = (Class<?>) soleParamType;
				}
				if (Collection.class.isAssignableFrom(soleParamClass)) {
					typeMap.put("collection", soleParamType);
					if (List.class.isAssignableFrom(soleParamClass)) {
						typeMap.put("list", soleParamType);
					}
				}
			}
		}
	}
	
	private String getActualParamName(Method method, int paramIndex) {
		return ParamNameUtil.getParamNames(method).get(paramIndex);
	}
	
	private static boolean isSpecialParameter(Class<?> clazz) {
		return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
	}
	
	public String[] getNames() {
		return names.values().toArray(new String[0]);
	}
	
	public Object getNamedParams(Object[] args) {
		final int paramCount = names.size();
		if (args == null || paramCount == 0) {
			return null;
		}
		if (!hasParamAnnotation && paramCount == 1) {
			Object value = args[names.firstKey()];
			return wrapToMapIfCollection(value, useActualParamName ? names.get(names.firstKey()) : null);
		} else {
			final Map<String, Object> param = new ParamMap<>();
			int i = 0;
			for (Map.Entry<Integer, String> entry : names.entrySet()) {
				param.put(entry.getValue(), args[entry.getKey()]);
				// add generic param names (param1, param2, ...)
				final String genericParamName = i < 10 ? GENERIC_NAME_CACHE[i] : GENERIC_NAME_PREFIX + (i + 1);
				// ensure not to override parameter named with @Param
				if (!names.containsValue(genericParamName)) {
					param.put(genericParamName, args[entry.getKey()]);
				}
				i++;
			}
			return param;
		}
	}
	
	public Type getType(String name) {
		PropertyTokenizer propertyTokenizer = new PropertyTokenizer(name);
		String unindexed = propertyTokenizer.getName();
		Type type = typeMap.get(unindexed);
		
		if (type == null && unindexed.startsWith(GENERIC_NAME_PREFIX)) {
			try {
				Integer paramIndex = Integer.valueOf(unindexed.substring(GENERIC_NAME_PREFIX.length())) - 1;
				unindexed = names.get(paramIndex);
				if (unindexed != null) {
					type = typeMap.get(unindexed));
				}
			} catch (NumberFormatException e) {
				// user mistake
			}
		}
		
		if (propertyTokenizer.getIndex() != null) {
			if (type instanceof ParameterizedType) {
				Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
				return typeArgs[0];
			} else if (type instanceof Class<?> && ((Class<?>) type).isArray()) {
				return ((Class<?>) type).getComponentType();
			}
		}
		return type;
	}
	
	public static Object wrapToMapIfCollection(Object object, String actualParamName) {
		if (object instanceof Collection) {
			ParamMap<Object> map = new ParamMap<>();
			map.put("collection", object);
			if (object instanceof List) {
				map.put("list", object);
			}
			Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
			return map;
		}
		if (object != null && object.getClass().isArray()) {
			ParamMap<Object> map = new ParamMap<>();
			map.put("array", object);
			Optional.ofNullable(actualParamName).ifPresent(name -> map.put(name, object));
			return map;
		}
		return object;
	}
	
	public boolean isUseParamMap() {
		return useParamMap;
	}
}
