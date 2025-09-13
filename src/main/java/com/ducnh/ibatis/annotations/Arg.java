package com.ducnh.ibatis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.UnknownTypeHandler;

import java.lang.annotation.Repeatable;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ConstructorArgs.class)
public @interface Arg {
	
	/**
	 * Returns whether id column or not.
	 * 
	 * @return {@code true} if id column; {@code false} if otherwise
	 */
	boolean id() default false;
	
	String column() default "";
	
	Class<?> javaType() default void.class;
	
	JdbcType jdbcType() default JdbcType.UNDEFINED;
	
	Class<? extends TypeHandler> typeHanlder() default UnknownTypeHandler.class;
	
	String select() default "";
	
	String resultMap() default "";
	
	String name() default "";
	
	String columnPrefix() default "";
}
