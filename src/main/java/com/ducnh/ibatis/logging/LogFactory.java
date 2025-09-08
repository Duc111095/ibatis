package com.ducnh.ibatis.logging;

import java.lang.reflect.Constructor;
import java.util.concurrent.locks.ReentrantLock;

public final class LogFactory {

	public static final String MARKER = "MYBATIS";
	
	private static final ReentrantLock lock = new ReentrantLock();
	private static Constructor<? extends Log> logConstructor;
	
	static {
		tryImplementation(LogFactory::useSlf4jLogging);
		tryImplementation(LogFactory::useCommonsLogging);
		tryImplementation(LogFactory::useLog4J2Logging);
		tryImplementation(LogFactory::useLog4JLogging);
		tryImplementation(LogFactory::useJdkLogging);
		tryImplementation(LogFactory::useNoLogging);
	}
	
	private LogFactory() {
		
	}
	
	public static Log getLog(Class<?> clazz) {
		return getLog(clazz.getName());
	}
	
	public static Log getLog(String logger) {
		try {
			return logConstructor.newInstance(logger);
		} catch (Throwable t) {
			throw new LogException("Error creating logger for logger " + logger + ". Cause: " + t, t); 
		}
	}
	
	public static void useCustomLogging(Class<? extends Log> clazz) {
		setImplementation(clazz);
	}
	
	public static void useSlf4jLogging() {
		setImplementation(com.ducnh.ibatis.logging.slf4j.Slf4jImpl.class);
	}
	
	public static void useCommonsLogging() {
		setImplementation(com.ducnh.ibatis.logging.commons.JakartaCommonsLoggingImpl.class);
	}
	
	public static void useLog4JLogging() {
		setImplementation(com.ducnh.ibatis.logging.log4j.Log4jImpl.class);
	}
	
	public static void useLog4J2Logging() {
		setImplementation(com.ducnh.ibatis.logging.log4j2.Log4j2Impl.class);
	}
	
	public static void useJdkLogging() {
		setImplementation(com.ducnh.ibatis.logging.jdk14.Jdk14LoggingImpl.class);
	}
	
	public static void useStdOutLogging() {
		setImplementation(com.ducnh.ibatis.logging.stdout.StdOutImpl.class);
	}
	
	public static void useNoLogging() {
		setImplementation(com.ducnh.ibatis.logging.nologging.NoLoggingImpl.class);
	}
	
	
	private static void tryImplementation(Runnable runnable) {
		if (logConstructor == null) {
			try {
				runnable.run();
			} catch (Throwable t) {
				// ignore
			}
		}
	}
	
	private static void setImplementation(Class<? extends Log> implClass) {
		lock.lock();
		try {
			Constructor<? extends Log> candidate = implClass.getConstructor(String.class);
			Log log = candidate.newInstance(LogFactory.class.getName());
			if (log.isDebugEnabled()) {
				log.debug("Logging initialized using '" + implClass + "' adapter.");
			}
			logConstructor = candidate;
		} catch (Throwable t) {
			throw new LogException("Error setting Log implementation. Cause: " + t, t);
		} finally {
			lock.unlock();
		}
	}
}
