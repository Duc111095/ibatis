package com.ducnh.ibatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.ducnh.ibatis.reflection.ExceptionUtil;
import com.ducnh.ibatis.session.SqlSession;

public class MapperProxy<T> implements InvocationHandler, Serializable {

	private static final long serialVersionUID = -4724728412955527868L;
	private static final Method privateLookupInMethod;
	private final SqlSession sqlSession;
	private final Class<T> mapperInterface;
	private final Map<Method, MapperMethodInvoker> methodCache;
	
	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethodInvoker> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}
	
	static {
		try {
			privateLookupInMethod = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(
				"There is no 'privateLookupIn(Class, Lookup)' method in java.lang.invoke.MethodHandles.", e);
		}
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, args);
			}
			return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}
	
	private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
		try {
			return methodCache.computeIfAbsent(method, m -> {
				if (!m.isDefault()) {
					return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
				}
				try {
					return new DefaultMethodInvoker(getMethodHandleJava9(method));
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
 			});
		} catch (RuntimeException re) {
			Throwable cause = re.getCause();
			throw cause == null ? re : cause;
		}
	}
	
	private MethodHandle getMethodHandleJava9(Method method) 
		throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		final Class<?> declaringClass = method.getDeclaringClass();
		return ((Lookup) privateLookupInMethod.invoke(null, declaringClass, MethodHandles.lookup())).findSpecial(
				declaringClass, method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
				declaringClass);
	}
	
	private static class PlainMethodInvoker implements MapperMethodInvoker {
		private final MapperMethod mapperMethod;
		
		public PlainMethodInvoker(MapperMethod mapperMethod) {
			this.mapperMethod = mapperMethod;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
			return mapperMethod.execute(sqlSession, args);
		}
	}
	
	private static class DefaultMethodInvoker implements MapperMethodInvoker {
		private final MethodHandle methodHandle;
		
		public DefaultMethodInvoker(MethodHandle methodHandle) {
			this.methodHandle = methodHandle;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
			return methodHandle.bindTo(proxy).invokeWithArguments(args);
		}
	}
}
