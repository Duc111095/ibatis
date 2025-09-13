package com.ducnh.ibatis.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.ducnh.ibatis.mapping.ResultSetType;
import com.ducnh.ibatis.mapping.StatementType;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Options.List.class)
public @interface Options {

	enum FlushCachePolicy {
		DEFAULT,
		TRUE,
		FALSE
	}
	
	boolean useCache() default true;
	
	FlushCachePolicy flushCache() default FlushCachePolicy.DEFAULT;
	
	ResultSetType resultSetType() default ResultSetType.DEFAULT;
	
	StatementType statementType() default StatementType.PREPARED;
	
	int fetchSize() default -1;
	
	int timeout() default -1;
	
	boolean useGeneratedKeys() default false;
	
	String keyProperty() default "";
	
	String keyColumn() default "";
	
	String resultSets() default "";
	
	String databaseId() default "";
	
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface List {
		Options[] value();
	}
}
