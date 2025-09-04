package com.ducnh.ibatis.reflection.property;

import java.lang.reflect.Field;

import com.ducnh.ibatis.reflection.Reflector;

public final class PropertyCopier {
	
	private PropertyCopier() {
		
	}
	
	public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
		Class<?> parent = type;
		while (parent != null) {
			final Field[] fields = parent.getDeclaredFields();
			for (Field field : fields) {
				try {
					try {
						field.set(destinationBean, field.get(sourceBean));
					} catch (IllegalAccessException e) {
						if (!Reflector.canControlMemberAccessible()) {
							throw e;
						}
						field.setAccessible(true);
						field.set(destinationBean, field.get(sourceBean));
					}
				} catch (Exception e) {
					
				}
			}
			parent = parent.getSuperclass();
		}
	}
}
