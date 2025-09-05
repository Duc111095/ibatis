package com.ducnh.ibatis.reflection;

import java.util.Optional;

public abstract class OptionalUtil {
	public static Object ofNullable(Object value) {
		return Optional.ofNullable(value);
	}
	
	private OptionalUtil() {
		
	}
}
