package com.ducnh.ibatis.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

public class ExceptionUtil {
	private ExceptionUtil() {
		
	}
	
	public static Throwable unwrapThrowable(Throwable wrapper) {
		Throwable unwrapper = wrapper;
		while (true) {
			if (unwrapper instanceof InvocationTargetException) {
				unwrapper = ((InvocationTargetException) unwrapper).getTargetException();
			} else if (unwrapper instanceof UndeclaredThrowableException) {
				unwrapper = ((UndeclaredThrowableException) unwrapper).getUndeclaredThrowable();
			} else {
				return unwrapper;
			}
		}
	}
}
