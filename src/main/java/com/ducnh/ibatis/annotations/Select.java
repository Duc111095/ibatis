package com.ducnh.ibatis.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Select.List.class)
public @interface Select {
	
	String[] value();
	
	String databaseId() default "";
	
	boolean affectData() default false;
	
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface List {
		Select[] value();
	}
}
