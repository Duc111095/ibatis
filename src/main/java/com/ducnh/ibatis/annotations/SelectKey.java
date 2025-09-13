package com.ducnh.ibatis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.ducnh.ibatis.mapping.StatementType;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(SelectKey.List.class)
public @interface SelectKey {
	
	String[] statement();
	
	String keyProperty();
	
	String keyColumn() default "";
	
	boolean before();
	
	Class<?> returnType();
	
	StatementType statementType() default StatementType.PREPARED;
	
	String databaseId() default "";
	
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface List {
		SelectKey[] value();
	}
}
