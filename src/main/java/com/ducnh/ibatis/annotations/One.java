package com.ducnh.ibatis.annotations;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.ducnh.ibatis.mapping.FetchType;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface One {
	String columnPrefix() default "";
	
	String resultMap() default "";
	
	String select() default "";
	
	FetchType fetchType() default FetchType.DEFAULT;
} 
