package com.ducnh.ibatis.reflection.invoker;

import java.lang.reflect.Field;

import com.ducnh.ibatis.reflection.Reflector;

public class GetFieldInvoker implements Invoker{

	private final Field field;
	
	public GetFieldInvoker(Field field) {
		this.field = field;
	}
	
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException {
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			if (Reflector.canControlMemberAccessible()) {
				field.setAccessible(true);
				return field.get(target);
			}
			
			throw e;
		}
	}

	@Override
	public Class<?> getType() {
		return field.getType();
	}
}
