package com.ducnh.ibatis.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;


/**
 * The annotation that indicate the method signature
 * 
 * @see Intercepts
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Signature {
	/**
	 * Returns the java type.
	 * 
	 * @return the java type
	 */
	Class<?> type();
	
	/**
	 * Returns the method name.
	 * 
	 * @return the method name
	 */
	String method();
	
	/**
	 * Returns java types for method argument
	 * 
	 * @return java types for method argument
	 */
	Class<?>[] args();
}
