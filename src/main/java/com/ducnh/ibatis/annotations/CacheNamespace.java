package com.ducnh.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespace {
	Class<? extends Cache> implementation() default PerpetualCache.class;
	
	Class<? extends Cache> eviction() default LruCache.class;
	
	long flushInterval() default 0;
	
	int size() default 1024;
	
	boolean readWrite() default true;
	
	boolean blocking() default true;
	
	Property[] properties() default {};
}
