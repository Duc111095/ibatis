package com.ducnh.ibatis.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

/**
 * The annotation that specify target methods to intercept.
 * <p>
 * <b> How to use: </b>
 * <pre>{@code
 * @Intercepts(value = {@Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class })
 * public class ExamplePlugin implements Interceptor {
 * 		@Override
 * 		public Object intercept(Invocation invocation) throws Throwable {
 *		// implement pre-processing if needed
 *	Object returnObject = invocation.proceed();
 *	// implement post-processing if needed
 *	return returnObject;
 *	}				
 * }
 *}</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Intercepts {
	/**
	 * Returns method signatures to intercept.
	 * 
	 * @return method signatures
	 */
	Signature[] value();
}
