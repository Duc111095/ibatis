package com.ducnh.ibatis.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
impo
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import java.util.List;

import com.ducnh.ibatis.reflection.invoker.Invoker;


public class Reflector {
	
	private static final MethodHandle isRecordMethodHandle = getIsRecordMethodHandle();
	private final Type type;
	private final Class<?> clazz;
	private final String[] readablePropertyNames;
	private final String[] writablePropertyNames;
	private final Map<String, Invoker> setMethods = new HashMap<>();
	private final Map<String, Invoker> getMethods = new HashMap<>();
	private final Map<String, Entry<Type, Class<?>>> setTypes = new HashMap<>();
	private final Map<String, Entry<Type, Class<?>>> getTypes = new HashMap<>();
	private Constructor<?> defaultConstructor;
	
	private final Map<String, String> caseInsensitivePropertyMap = new HashMap<>();
	private static final Entry<Type, Class<?>> nullEntry = new AbstractMap.SimpleImmutableEntry<>(null, null);
	
	public Reflector(Type type) {
		this.type = type;
		if (type instanceof ParameterizedType) {
			this.clazz = (Class<?>) ((ParameterizedType) type).getRawType();
		} else {
			this.clazz = (Class<?>) type;
		}
		addDefaultConstructor(clazz);
		Method[] classMethods = getClassMethods(clazz);
		if (isRecord(clazz)) {
			addRecordGetMethods(classMethods);
		} else {
			addGetMethods(classMethods);
			addSetMethods(classMethods);
			addFields(clazz);
		}
		readablePropertyNames = getMethods.keySet().toArray(new String[0]);
		writablePropertyNames = setMethods.keySet().toArray(new String[0]);
		for (String propName : readablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
		for (String propName : writablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
	}
	
	private void addRecordGetMethods(Method[] methods) {
		Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0)
			.forEach(m -> addGetMethod(m.getName(), m, false));
	}
	
	private void addDefaultConstructor(Class<?> clazz) {
		Constructor<?>[] constructors = clazz.getDeclaredConstructors();
		Arrays.stream(constructors).filter(constructor -> constructor.getParameterTypes().length == 0).findAny()
			.ifPresent(constructor -> this.defaultConstructor = constructor);
	}
	
	private void addGetMethods(Method[] methods) {
		Map<String, List<Method>> conflictingGetters = new HashMap<>();
		Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 0 && PropertyNamer.isGetter(m.getName()))
			.forEach(m -> addMethodConflict(conflictingGetters, PropertyNamer.methodToProperty(m.getName()), m));
		resolveGetterConflicts(conflictingGetters);
	}
	
	private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
		for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
			Method winner = null;
			String propName = entry.getKey();
			boolean isAmbiguous = false;
			for (Method candidate : entry.getValue()) {
				if (winner == null) {
					winner = candidate;
					continue;
				}
				Class<?> winnerType = winner.getReturnType();
				Class<?> candidateType = candidate.getReturnType();
				if (candidateType.equals(winnerType)) {
					if (!boolean.class.equals(candidateType)) {
						isAmbiguous = true;
						break;
					}
					if (candidate.getName().startsWith("is")) {
						winner = candidate;
					}
				} else if (candidateType.isAssignableFrom(winnerType)) {
					
				} else if (winnerType.isAssignableFrom(candidateType)) {
					winner = candidate;
				} else {
					isAmbiguous = true;
					break;
				}
			}
			addGetMethod(propName, winner, isAmbiguous);
		}
	}
	
	private void addGetMethod(String name, Method method, boolean isAmbiguous) {
		MethodInvoker invoker = isAmbiguous ? new AmbiguousMethodInvoker(method, MessageFormat.format(
				"Illegal overloaded getter method with ambiguous type for property ''{0}'' in class ''{1}''. This breaks the JavaBeans specification and can cause unpredictable results.",
				name, method.getDeclaringClass().getName())) : new MethodInvoker(method);
		getMethods.put(name, invoker);
		Type returnType = TypeParameterResolver.resolveReturnType(method, type);
		getTypes.put(name, Map.entry(returnType, typeToClass(returnType))));
	}
	
	private void addSetMethods(Method[] methods) {
		Map<String, List<Method>> conflictingSetters = new HashMap<>();
		Arrays.stream(methods).filter(m -> m.getParameterTypes().length == 1 && PropertyNamer.isSetter(m.getName()))
			.forEach(m -> addMethodConflict(conflictingSetters, PropertyNamer.methodToProperty(m.getName()), m));
		resolveSetterConflicts(conflictingSetters);
	}
	
	private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
		if (isValidPropertyName(name)) {
			List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
			list.add(method);
		}
	}
	
	private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
		for (Entry<String, List<Method>> entry : conflictingSetters.entrySet()) {
			String propName = entry.getKey();
			List<Method> setters = entry.getValue();
			Class<?> getterType = getTypes.getOrDefault(propName, nullEntry).getValue();
			boolean isGetterAmbiguous = getMethods.get(propName) instanceof AmbiguousMethodInvoker;
			boolean isSetterAmbiguous = false;
			Method match = null;
			for (Method setter : setters) {
				if (!isGetterAmbiguous && setter.getParameterTypes()[0].equals(getterType)) {
					match = setter;
					break;
				}
				if (!isSetterAmbiguous) {
					match = pickBetterSetter(match, setter, propName);
					isSetterAmbiguous = match == null;
				}
			}
			if (match != null) {
				addSetMethod(propName, match);
			}
		}
	}
	
	private Method pickBetterSetter(Method setter1, Method setter2, String property) {
		if (setter1 == null) {
			return setter2;
		}
		Class<?> paramType1 = setter1.getParameterTypes()[0];
		Class<?> paramType2 = setter2.getParameterTypes()[0];
		if (paramType1.isAssignableFrom(paramType2)) {
			return setter2;
		}
		if (paramType2.isAssignableFrom(paramType1)) {
			return setter1;
		}
		MethodInvoker invoker = new AmbiguousMethodInvoker(setter1,
			MessageFormat.format(
				"Ambiguous setters defined for property ''{0}'' in class ''{1}'' with types ''{2}'' and ''{3}''.", property,
				setter2.getDeclaringClass().getName(), paramType1.getName(), paramType2.getName()));
		setMethods.put(property, invoker);
		Type[] paramTypes = TypeParameterResolver.resolveParamTypes(setter1, type);
		setTypes.put(property, Map.entry(paramTypes[0], typeToClass(paramTypes[0])));
		return null;
	}
	
	private void addSetMethod(String name, Method method) {
		MethodInvoker invoker = new MethodInvoker(method);
		setMethods.put(name, invoker);
		Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
		setTypes.put(name, Map.entry(paramTypes[0], typeToClass(paramTypes[0])));
	}
	
	private Class<?> typeToClass(Type src) {
		if (src instanceof Class) {
			return (Class<?>) src;
		} else if (src instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) src).getRawType();
		} else if (src instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) src).getGenericComponentType();
			if (componentType instanceof Class) {
				return Array.newInstance((Class<?>) componentType, 0).getClass();
			} else {
				Class<?> componentClass = typeToClass(componentType);
				return Array.newInstance(componentClass, 0).getClass();
			}
		}
		return Object.class;
	}
	
	private void addFields(Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (!setMethods.containsKey(field.getName())) {
				int modifiers = field.getModifiers();
				if (!Modifier.isFinal(modifiers) || !Modifier.isStatic(modifiers)) {
					addSetField(field);
				}
			}
			if (!getMethods.containsKey(field.getName())) {
				addGetField(field);
			}
		}
		if (clazz.getSuperclass() != null) {
			addFields(clazz.getSuperclass());
		}
	}
	
	private void addSetField(Field field) {
		if (isValidPropertyName(field.getName())) {
			setMethods.put(field.getName(), new SetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			setTypes.put(field.getName(), Map.entry(fieldType, typeToClass(fieldType)));
		}
	}
	
	private void addGetField(Field field) {
		if (isValidPropertyName(field.getName())) {
			getMethods.put(field.getName(), new GetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			getTypes.put(field.getName(), Map.entry(fieldType, typeToClass(fieldType)));
		}
	}
	
	private boolean isValidPropertyName(String name) {
		return !name.startsWith("$") && !"serialVersionUID".equals(name) && !"class".equals(name);
	}
	
	private Method[] getClassMethods(Class<?> clazz) {
		Map<String, Method> uniqueMethods = new HashMap<>();
		Class<?> currentClass = clazz;
		while (currentClass != null && currentClass != Object.class) {
			addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());
			
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> anInterface : interfaces) {
				addUniqueMethods(uniqueMethods, anInterface.getMethods());
			}
			currentClass = currentClass.getSuperclass();
		}
		
		Collection<Method> methods = uniqueMethods.values();
		return methods.toArray(new Method[0]);
	}
	
	private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
		for (Method currentMethod : methods) {
			if (!currentMethod.isBridge()) {
				String signature = getSignature(currentMethod);
				if (!uniqueMethods.containsKey(signature)) {
					uniqueMethods.put(signature, currentMethod);
				}
			}
		}
	}
	
	private String getSignature(Method method) {
		StringBuilder sb = new StringBuilder();
		Class<?> returnType = method.getReturnType();
		sb.append(returnType.getName()).append('#');
		sb.append(method.getName());
		Class<?>[] parameters = method.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			sb.append(i == 0 ? ":" : ",").append(parameters[i].getName());
		}
		return sb.toString();
	}
	
	@SuppressWarnings({ "removal", "deprecation" })
	public static boolean canControlMemberAccessible() {
		try {
			SecurityManager securityManager = System.getSecurityManager();
			if (null != securityManager) {
				securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
			}
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}
	
	public Class<?> getType() {
		return clazz;
	}
	
	public Constructor<?> getDefaultConstructor() {
		if (defaultConstructor != null) {
			return defaultConstructor;
		}
		throw new ReflectionException("There is not default constructor for " + clazz);
	}
	
	public boolean hasDefaultConstructor() {
		return defaultConstructor != null;
	}
	
	public Invoker getSetInvoker(String propertyName) {
		Invoker method = setMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
		}
		return method;
	}
	
	public Invoker getGetInvoker(String propertyName) {
		Invoker method = getMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
		}
		return method;
	}
	
	public Class<?> getSetterType(String propertyName) {
		Class<?> clazz =  setTypes.get(propertyName).getValue();
		if (clazz != null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + clazz + "'");
		}
		return clazz;
	}
	
	public Entry<Type, Class<?>> getGenericSetterType(String propertyName) {
		return setTypes.computeIfAbsent(propertyName, k -> {
			throw new ReflectionException("There is not setter for property named '" + k + "' in '" + clazz + "'");
		});
	}
	
	public Class<?> getGetterType(String propertyName) {
		Class<?> clazz = getTypes.getOrDefault(propertyName, nullEntry).getValue();
		if (clazz == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
		}
		return clazz;
	}
	
	public Entry<Type, Class<?>> getGenericGetterType(String propertyName) {
		return getTypes.computeIfAbsent(propertyName, k -> {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + clazz + "'");
		});
	}
	
	
}
