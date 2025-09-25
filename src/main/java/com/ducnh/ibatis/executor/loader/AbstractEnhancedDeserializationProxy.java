package com.ducnh.ibatis.executor.loader;

public abstract class AbstractEnhancedDeserializationProxy {

	protected static final String FINALIZE_METHOD = "finalize";
	protected static final String WRITE_PLACE_METHOD = "writePlace";
	private final Class<?> type;
	private final Map<String, ResultLoaderMap.LoadPair> unloadedProperties;
	
}
