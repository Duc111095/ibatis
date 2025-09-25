package com.ducnh.ibatis.executor.resultset;

final class PendingCreationMetaInfo {

	private final Class<?> argumentType;
	private final PendingCreationKey pendingCreationKey;
	
	PendingCreationMetaInfo(Class<?> argumentType, PendingCreationKey pendingCreationKey) {
		this.argumentType = argumentType;
		this.pendingCreationKey = pendingCreationKey;
	}
	
	Class<?> getArgumentType() {
		return argumentType;
	}
	
	PendingCreationKey getPendingCreationKey() {
		return pendingCreationKey;
	}
	
	@Override
	public String toString() {
		return "PendingCreationMetaInfo{" + "argumentType=" + argumentType + ", pendingCreationKey=" + pendingCreationKey 
				+ "'}";
	}
}
