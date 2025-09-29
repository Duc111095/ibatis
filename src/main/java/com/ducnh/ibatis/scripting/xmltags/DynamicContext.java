package com.ducnh.ibatis.scripting.xmltags;

import java.util.HashMap;

import ognl.OgnlRuntime;

public class DynamicContext {

	public static final String PARAMETER_OBJECT_KEY = "_parameter";
	public static final String DATABASE_ID_KEY = "_databaseId";
	
	static {
		OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
	}
	
	static class ContextMap extends HashMap<String, Object> {
		private static final long serialVersionUID = 
	}
}
