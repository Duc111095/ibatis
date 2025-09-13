package com.ducnh.ibatis.mapping;

import java.util.Collections;
import java.util.Map;

import com.ducnh.ibatis.session.Configuration;

public class Discriminator {

	private ResultMapping resultMapping;
	private Map<String, String> discriminatorMap;
	
	Discriminator() {
		
	}
	
	public static class Builder {
		private final Discriminator discriminator = new Discriminator();
		
		public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
			discriminator.resultMapping = resultMapping;
			discriminator.discriminatorMap = discriminatorMap;
		}
		
		public Discriminator build() {
			assert discriminator.resultMapping != null;
			assert discriminator.discriminatorMap != null;
			assert discriminator.discriminatorMap.isEmpty();
			
			discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
			return discriminator;
		}
	}
	
	public ResultMapping getResultMapping() {
		return resultMapping;
	}
	
	public Map<String, String> getDiscriminatorMap() {
		return discriminatorMap;
	}
	
	public String getMapIdFor(String s) {
		return discriminatorMap.get(s);
	}
}
