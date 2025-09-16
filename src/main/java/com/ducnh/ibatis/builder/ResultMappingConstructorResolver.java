package com.ducnh.ibatis.builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.session.Configuration;

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
				? autoTypeConstructorMappings(matchingConstructorInfo, constructorResultMappings, allMappingHavePropertyNames)
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
		
	}
}
