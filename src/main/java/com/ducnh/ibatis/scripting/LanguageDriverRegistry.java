package com.ducnh.ibatis.scripting;

import java.util.HashMap;
import java.util.Map;

public class LanguageDriverRegistry {

	private final Map<Class<? extends LanguageDriver>, LanguageDriver> languageDriverMap = new HashMap<>();

	private Class<? extends LanguageDriver> defaultDriverClass;
	
	public void register(Class<? extends LanguageDriver> cls) {
		if (cls == null) {
			throw new IllegalArgumentException("null is not a valid Language Driver");
		}
		languageDriverMap.computeIfAbsent(cls, k -> {
			try {
				return k.getDeclaredConstructor().newInstance();
			} catch (Exception ex) {
				throw new ScriptingException("Failed to load language driver for " + cls.getName(), ex);
			}
		});
	}
	
	public void register(LanguageDriver instance) {
		if (instance == null) {
			throw new IllegalArgumentException("null is not valid language Driver");
		}
		Class<? extends LanguageDriver> cls = instance.getClass();
		if (!languageDriverMap.containsKey(cls)) {
			languageDriverMap.put(cls, instance);
		}
	}
	
	public LanguageDriver getDriver(Class<? extends LanguageDriver> cls) {
		return languageDriverMap.get(cls);
	}
	
	public LanguageDriver getDefaultDriver() {
		return languageDriverMap.get(getDefaultDriverClass());
	}
	
	public Class<? extends LanguageDriver> getDefaultDriverClass() {
		return defaultDriverClass;
	}
	
	public void setDefaultDriverClass(Class<? extends LanguageDriver> defaultDriverClass) {
		register(defaultDriverClass);
		this.defaultDriverClass = defaultDriverClass;
	}
}
