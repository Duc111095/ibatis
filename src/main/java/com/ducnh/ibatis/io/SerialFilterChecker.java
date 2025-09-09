package com.ducnh.ibatis.io;

import com.ducnh.ibatis.logging.LogFactory;

import java.security.Security;

import com.ducnh.ibatis.logging.Log;

public final class SerialFilterChecker {
	private static final Log log = LogFactory.getLog(SerialFilterChecker.class);
	
	private static final String JDK_SERIAL_FILTER = "jdk.serialFilter";
	private static final boolean SERIAL_FILTER_MISSING;
	private static boolean firstInvocation = true;
	
	static {
		Object serialFilter;
		try {
			Class<?> objectFilterConfig = Class.forName("java.io.ObjectInputFilter$Config");
			serialFilter = objectFilterConfig.getMethod("getSerialFilter").invoke(null);
		} catch (ReflectiveOperationException e) {
			serialFilter = System.getProperty(JDK_SERIAL_FILTER, Security.getProperty(JDK_SERIAL_FILTER));
		}
		SERIAL_FILTER_MISSING = serialFilter == null;
	}
	
	public static void check() {
		if (firstInvocation && SERIAL_FILTER_MISSING) {
			firstInvocation = false;
			log.warn(
				"As you are using functionality that deserializes object streams, it is recommended to define the JEP-290 serial filter check");		
		}
	}
	
	private SerialFilterChecker() {
		
	}
}
