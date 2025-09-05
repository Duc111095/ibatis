package com.ducnh.ibatis.reflection.invoker;

import java.lang.reflect.Field;

import com.ducnh.ibatis.reflection.Reflector;

public class SetFieldInvoker implements Invoker{
	private final Field field;
	
	public SetFieldInvoker(Field field) {
		this.field = field;
	}
	
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException {
		try {
			field.set(target, args[0]);
		} catch (IllegalAccessException e) {
			if (!Reflector.canControlMemberAccessible()) {
				throw e;
			}
			field.setAccessible(true);
			field.set(target, args[0]);
		}
		return null;
	}
	
	@Override
	public Class<?> getType() {
		return field.getType();
	}
}
