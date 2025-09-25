package com.ducnh.ibatis.executor.resultset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.mapping.ResultMap;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.reflection.ReflectionException;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;


final class PendingConstructorCreation {

	private final Class<?> resultType;
	private final List<Class<?>> constructorArgTypes;
	private final List<Object> constructorArgs;
	
	private final Map<Integer, PendingCreationMetaInfo> linkedCollectionMetaInfo;
	private final Map<PendingCreationKey, Collection<Object>> linkedCollectionsByKey;
	private final Map<PendingCreationKey, List<PendingConstructorCreation>> linkedCreationsByKey;

	PendingConstructorCreation(Class<?> resultType, List<Class<?>> types, List<Object> args) {
		final int maxSize = types.size();
		
		this.linkedCollectionMetaInfo = new HashMap<>(maxSize);
		this.linkedCollectionsByKey =  new HashMap<>(maxSize);
		this.linkedCreationsByKey = new HashMap<>(maxSize);
		
		this.resultType = resultType;
		this.constructorArgTypes = types;
		this.constructorArgs = args;
	}
	
	@SuppressWarnings("unchecked")
	Collection<Object> initializeCollectionForResultMapping(ObjectFactory objectFactory, ResultMap resultMap,
		ResultMapping constructorMapping, Integer index) {
		final Class<?> parameterType = constructorMapping.getJavaType();
		if (!objectFactory.isCollection(parameterType)) {
			throw new ReflectionException(
				"Cannot add a collection result to non-collection based resultMapping: " + constructorMapping);
		}
		
		return linkedCollectionsByKey.computeIfAbsent(new PendingCreationKey(constructorMapping), k -> {
			linkedCollectionMetaInfo.put(index, new PendingCreationMetaInfo(resultMap.getType(), k));
			return (Collection<Object>) objectFactory.create(parameterType);
		});
	} 
	
	void linkCreation(ResultMapping constructorMapping, PendingConstructorCreation pcc) {
		final PendingCreationKey creationKey = new PendingCreationKey(constructorMapping);
		final List<PendingConstructorCreation> pendingConstructorCreations = linkedCreationsByKey
			.computeIfAbsent(creationKey, k -> new ArrayList<>());
		
		if (pendingConstructorCreations.contains(pcc)) {
			throw new ExecutorException("Cannot link inner constructor creation with the same value, MyBatis internal error!");
		}
		
		pendingConstructorCreations.add(pcc);
	}
	
	void linkCollectionValue(ResultMapping constructorMapping, Object value) {
		if (value == null) {
			return;
		}
		
		linkedCollectionsByKey.computeIfAbsent(new PendingCreationKey(constructorMapping), k -> {
			throw new ExecutorException("Cannot link collection value for key: " + constructorMapping 
				+ ", resultMap has not been seen/initialzed yet! Mybatis internal error!");
		}).add(value);
	}
	
	@Override
	public String toString() {
		return "PendingConstructorCreation(" + this.hashCode() + "){" + "resultType=" + resultType + '}';
	}
	
	Object create(ObjectFactory objectFactory) {
		final List<Object> newArguments = new ArrayList<>(constructorArgs.size());
		for (int i = 0; i < constructorArgs.size(); i++) {
			final PendingCreationMetaInfo creationMetaInfo = linkedCollectionMetaInfo.get(i);
			final Object existingArg = constructorArgs.get(i);
			
			if (creationMetaInfo == null) {
				newArguments.add(existingArg);
				continue;
			}
			
			// time to finally build this collection
			final PendingCreationKey pendingCreationKey = creationMetaInfo.getPendingCreationKey();
			final List<PendingConstructorCreation> linkedCreations = linkedCreationsByKey.get(pendingCreationKey);
			if (linkedCreations != null) {
				@SuppressWarnings("unchecked")
				final Collection<Object> emptyCollection = (Collection<Object>) existingArg;
				
				for (PendingConstructorCreation linkedCreation : linkedCreations) {
					emptyCollection.add(linkedCreation.create(objectFactory));
				}
				
				newArguments.add(emptyCollection);
				continue;
			}
			
			// handle the base collection
			newArguments.add(existingArg);
		}
		return objectFactory.create(resultType, constructorArgTypes, newArguments);
	}
}
