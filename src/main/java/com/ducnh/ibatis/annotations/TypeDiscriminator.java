package com.ducnh.ibatis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.UnknownTypeHandler;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TypeDiscriminator {

	String column();
	
	Class<?> javaType() default void.class;
	
	JdbcType jdbcType() default JdbcType.UNDEFINED;
	
	Class<? extends TypeHandler> typeHandler() default UnknownTypeHandler.class;
	
	Case[] cases();
}
