package com.ducnh.ibatis.executor.resultset;

import java.util.Objects;

import com.ducnh.ibatis.mapping.ResultMapping;

final class PendingCreationKey {

	private final String resultMapId;
	private final String constructorColumnPrefix;
	
	PendingCreationKey(ResultMapping constructorMapping) {
		this.resultMapId = constructorMapping.getNestedResultMapId();
		this.constructorColumnPrefix = constructorMapping.getColumnPrefix();
	}
	
	String getConstructorColumnPrefix() {
		return constructorColumnPrefix;
	}
	
	String getResultMapId() {
		return resultMapId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		
		PendingCreationKey that = (PendingCreationKey) o;
		return Objects.equals(this.getConstructorColumnPrefix(), that.getConstructorColumnPrefix()) 
			&& Objects.equals(resultMapId, that.resultMapId);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(resultMapId, constructorColumnPrefix);
	}
	
	@Override
	public String toString() {
		return "PendingCreationKey{" + "resultMapId='" + resultMapId + '\'' + ", constructorColumnPrefix='"
			+ constructorColumnPrefix + '\'' +'}';
	}
}
