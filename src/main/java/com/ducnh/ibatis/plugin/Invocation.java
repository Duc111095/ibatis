package com.ducnh.ibatis.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.executor.resultset.ResultSetHandler;
import com.ducnh.ibatis.executor.statement.StatementHandler;

public class Invocation {

	private static final List<Class<?>> targetClasses = Arrays.asList(Executor.class, ParameterHandler.class,
		ResultSetHandler.class, StatementHandler.class);
	private final Object target;
	private final Method method;
	private final Object[] args;
	
	
	public Invocation(Object target, Method method, Object[] args) {
		if (!targetClasses.contains(method.getDeclaringClass())) {
			throw new IllegalArgumentException("Method '" + method + "' is not supported as a plugin target.");
		}
		this.target = target;
		this.method = method;
		this.args = args;
	}
	
	public Object getTarget() {
		return target;
	}
	
	public Method getMethod() {
		return method;
	}
	
	public Object[] getArgs() {
		return args;
	}
	
	public Object proceed() throws InvocationTargetException, IllegalAccessException {
		return method.invoke(target, args);
	}
} 
