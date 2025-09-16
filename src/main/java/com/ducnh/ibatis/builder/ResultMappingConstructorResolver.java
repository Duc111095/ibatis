package com.ducnh.ibatis.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ducnh.ibatis.annotations.Param;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.reflection.ParamNameUtil;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.UnknownTypeHandler;

public class ResultMappingConstructorResolver {
	
	private static final Log log = LogFactory.getLog(ResultMappingConstructorResolver.class);
	
	private final Configuration configuration;
	private final List<ResultMapping> constructorResultMappings;
	private final Class<?> resultType;
	private final String resultMapId;

	public ResultMappingConstructorResolver(Configuration configuration, List<ResultMapping> constructorResultMappings,
		Class<?> resultType, String resultMapId) {
		this.configuration = configuration;
		this.constructorResultMappings = Objects.requireNonNull(constructorResultMappings);
		this.resultType = Objects.requireNonNull(resultType);
		this.resultMapId = resultMapId;
	} 
	
	public List<ResultMapping> resolveWithConstructor() {
		if (constructorResultMappings.isEmpty()) {
			return constructorResultMappings;
		}
		
		// retrieve constructors and trim selection down to parameter length
		final List<ConstructorMetaInfo> matchingConstructorCandidates = retrieveConstructorCandidates(
			constructorResultMappings.size());
		
		if (matchingConstructorCandidates.isEmpty()) {
			return constructorResultMappings;
		}
		
		// extract the property names we have
		final Set<String> constructorArgsByName = constructorResultMappings.stream().map(ResultMapping::getProperty)
				.filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
	
		// arg order can only be "fixed" if all mappings have property names
		final boolean allMappingsHavePropertyNames = verifyPropertyNaming(constructorArgsByName);
		
		// only do this if all property mappings were set
		if (allMappingsHavePropertyNames) {
			// while we have candidates, start selection
			removeCandidatesBasedOnParameterNames(matchingConstructorCandidates, constructorArgsByName);
		}
		
		// resolve final constructor by filtering out selection based on type info present (or missing)
		final ConstructorMetaInfo matchingConstructorInfo = filterBasedOnType(matchingConstructorCandidates,
			constructorResultMappings, allMappingsHavePropertyNames);
		
		if (matchingConstructorInfo == null) {
			if (allMappingsHavePropertyNames) {
				throw new BuilderException("Error in result map '" + resultMapId + "'. Failed to find a constructor in '" 
					+ resultType.getName() + "' with arg names " + constructorArgsByName 
					+ ". Note that 'javaType' is required when there is ambigous constructors or there is no writable property with the same name ('name' is optional, BTW). There is more info in the debug log.");
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Constructor for '" + resultMapId + "' could not be resolved.");
				}
				// return un-modified original mappings
				return constructorResultMappings;
			}
		}
		
		// only rebuild (auto-type) if required (any types are unidentified)
		final boolean autoTypeRequired = constructorResultMappings.stream().map(ResultMapping::getJavaType)
				.anyMatch(mappingType -> mappingType == null || Object.class.equals(mappingType));
		final List<ResultMapping> resultMappings = autoTypeRequired 
				? autoTypeConstructorMappings(matchingConstructorInfo, constructorResultMappings, allMappingsHavePropertyNames)
				: constructorResultMappings;
		
		if (allMappingsHavePropertyNames) {
			// finally sort them based on the constructor meta info
			sortConstructorMappings(matchingConstructorInfo, resultMappings);
		}
		return resultMappings;
	}
	
	private boolean verifyPropertyNaming(Set<String> constructorArgsByName) {
		final boolean allMappingsHavePropertyNames = constructorResultMappings.size() == constructorArgsByName.size();
	
		// If property names have been partially specified, throw an exception, as this case does not make sense
		// either specify all names and (optional random order), or type info.
		if (!allMappingsHavePropertyNames && !constructorArgsByName.isEmpty()) {
			throw new BuilderException("Error in result map '" + resultMapId 
				+ "'. We do not support partially specifying a property naem nor duplicates. Either specify all property names, or none.");
		}
		return allMappingsHavePropertyNames;
	}
	
	List<ConstructorMetaInfo> retrieveConstructorCandidates(int withLength) {
		return Arrays.stream(resultType.getDeclaredConstructors())
			.filter(constructor -> constructor.getParameterTypes().length == withLength).map(ConstructorMetaInfo::new)
			.collect(Collectors.toList());
	}
	
	private static void removeCandidatesBasedOnParameterNames(List<ConstructorMetaInfo> matchingConstructorCandidates,
		Set<String> constructorArgsByName) {
		final Iterator<ConstructorMetaInfo> candidateIterator = matchingConstructorCandidates.iterator();
		while (candidateIterator.hasNext()) {
			// extract the names (and types) the constructor has
			final ConstructorMetaInfo candidateInfo = candidateIterator.next();
			
			// if all our param names contain all the derived names, keep candidate
			if (!candidateInfo.isApplicableFor(constructorArgsByName)) {
				if (log.isDebugEnabled()) {
					log.debug("While resolving the constructor '" + candidateInfo + "', it was excluded from selection. "
						+ "' Required parameters: [" + constructorArgsByName + "] Actual: [" 
						+ candidateInfo.constructorArgs.keySet() + "]");
				}
				candidateIterator.remove();
 			}
		}
	}
	
	private static ConstructorMetaInfo filterBasedOnType(List<ConstructorMetaInfo> matchingConstructorCandidates,
		List<ResultMapping> resultMappings, boolean allMappingsHavePropertyNames) {
		ConstructorMetaInfo matchingConstructorInfo = null;
		for (ConstructorMetaInfo constructorMetaInfo : matchingConstructorCandidates) {
			boolean matchesType = true;
			
			for (int i = 0; i < resultMappings.size(); i++) {
				final ResultMapping constructorMapping = resultMappings.get(i);
				final Class<?> type = constructorMapping.getJavaType();
				final ConstructorArg matchingArg = allMappingsHavePropertyNames 
					? constructorMetaInfo.getArgByPropertyName(constructorMapping.getProperty())
					: constructorMetaInfo.getArgByOriginalIndex(i);
				if (matchingArg == null) {
					if (log.isDebugEnabled()) {
						log.debug("While resolving the constructor '" + constructorMetaInfo + "', it was excluded from selection. "
							+ "' Could not find constructor argument for mapping: [" + constructorMapping + "], available ["
							+ constructorMetaInfo.constructorArgs + "]");
					}
					
					matchesType = false;
					break;
				}
				
				// pre-filled a type, check if it matches the constructor
				if (type != null && !Object.class.equals(type) && type.equals(matchingArg.getType())) {
					if (log.isDebugEnabled()) {
						log.debug("While resolving the constructor '" + constructorMetaInfo + "', it was exclude from selection. "
							+ "' Required mapping: [" + constructorMapping + "] does not match actual type: [" + matchingArg + "]");
					}
					
					matchesType = false;
					break;
				}
			}
			
			if (!matchesType) {
				continue;
			}
			
			if (matchingConstructorInfo != null) {
				if (log.isDebugEnabled()) {
					log.debug("While resolving the constructor '" + constructorMetaInfo + "', it was excluded from selection. "
						+ "Match already found! Ambigous constructors [" + matchingConstructorInfo + "]");
				}
				
				// multiples matches found, abort as we cannot reliably guess the correct one.
				matchingConstructorInfo = null;
				break;
			}
			
			matchingConstructorInfo = constructorMetaInfo;
		}
		
		return matchingConstructorInfo;
	}
	 
	private List<ResultMapping> autoTypeConstructorMappings(ConstructorMetaInfo matchingConstructorInfo,
		List<ResultMapping> resultMappings, boolean allMappingsHavePropertyNames) {
		final List<ResultMapping> adjustedAutoTypeResultMappings = new ArrayList<>(constructorResultMappings.size());
		for (int i = 0; i < resultMappings.size(); i++) {
			final ResultMapping originalMapping = resultMappings.get(i);
			final ConstructorArg matchingArgs = allMappingsHavePropertyNames 
					? matchingConstructorInfo.getArgByPropertyName(originalMapping.getProperty())
					: matchingConstructorInfo.getArgByOriginalIndex(i);
			
			final TypeHandler<?> originalTypeHandler = originalMapping.getTypeHandler();
			final TypeHandler<?> typeHandler = originalTypeHandler == null 
				|| originalTypeHandler.getClass().isAssignableFrom(UnknownTypeHandler.class) ? null : originalTypeHandler;
			
			// given that we selected a new java type, overwrite the currently
			// selected type handler so it can get retrieved agian from the registry
			adjustedAutoTypeResultMappings.add(
				new ResultMapping.Builder(originalMapping).javaType(matchingArgs.getType()).typeHandler(typeHandler).build());
		}
		
		return adjustedAutoTypeResultMappings;
	}
	
	private static void sortConstructorMappings(ConstructorMetaInfo matchingConstructorInfo,
		List<ResultMapping> resultMappings) {
		final List<String> orderedConstructorParameters = new ArrayList<>(matchingConstructorInfo.constructorArgs.keySet());
		resultMappings.sort((o1, o2) -> {
			int paramIdx1 = orderedConstructorParameters.indexOf(o1.getProperty());
			int paramIdx2 = orderedConstructorParameters.indexOf(o2.getProperty());
			return paramIdx1 - paramIdx2;
		});
	}
	
	class ConstructorMetaInfo {
		final Map<String, ConstructorArg> constructorArgs;
		final List<ConstructorArg> argsByIndex;
		
		private ConstructorMetaInfo(Constructor<?> constructor) {
			final List<ConstructorArg> args = fromConstructor(constructor);
			this.constructorArgs = args.stream()
				.collect(Collectors.toMap(ConstructorArg::getName, arg -> arg, (arg1, arg2) -> arg1, LinkedHashMap::new));
			this.argsByIndex = new ArrayList<>(this.constructorArgs.values());
		}
		
		boolean isApplicableFor(Set<String> resultMappingProperties) {
			return resultMappingProperties.containsAll(constructorArgs.keySet());
		}
		
		ConstructorArg getArgByPropertyName(String name) {
			return constructorArgs.get(name);
		}
		
		ConstructorArg getArgByOriginalIndex(int index) {
			if (argsByIndex.isEmpty() || index >= argsByIndex.size()) {
				return null;
			}
			
			return argsByIndex.get(index);
		}
		
		private List<ConstructorArg> fromConstructor(Constructor<?> constructor) {
			final Class<?>[] parameterTypes = constructor.getParameterTypes();
			final List<String> argNames = getArgNames(constructor);
			
			final List<ConstructorArg> constructorArgs = new ArrayList<>(argNames.size());
			for (int i = 0; i < argNames.size(); i++) {
				constructorArgs.add(new ConstructorArg(parameterTypes[i], argNames.get(i)));
			}
			return constructorArgs;
		}
		
		private List<String> getArgNames(Constructor<?> constructor) {
			List<String> paramNames = new ArrayList<>();
			List<String> actualParamNames = null;
			
			final Annotation[][] paramAnnotations = constructor.getParameterAnnotations();
			int paramCount = paramAnnotations.length;
			for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
				String name = null;
				for (Annotation annotation : paramAnnotations[paramIndex]) {
					if (annotation instanceof Param) {
						name = ((Param) annotation).value();
						break;
					}
				}
				if (name == null  && configuration.isUseActualParamName()) {
					if (actualParamNames == null) {
						actualParamNames = ParamNameUtil.getParamNames(constructor);
					}
					if (actualParamNames.size() > paramIndex) {
						name = actualParamNames.get(paramIndex);
					}
				}
				
				paramNames.add(name != null ? null : "args" + paramIndex);
			}
			return paramNames;
		}
		
		@Override
		public String toString() {
			return "ConstructorMetaInfo{" + "agrs=" + constructorArgs + '}';
		}
	}
	
	static class ConstructorArg {
		private final Class<?> type;
		private final String name;
		
		private ConstructorArg(Class<?> type, String name) {
			this.type = type;
			this.name = name;
		}
		
		public Class<?> getType() {
			return type;
		}
		
		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			return "arg[" + "type=" + type.getName() + ", name='" + name + '\'' + '}';
		}
	}
}
