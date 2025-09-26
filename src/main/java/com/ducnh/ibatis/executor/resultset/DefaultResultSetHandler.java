package com.ducnh.ibatis.executor.resultset;

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


import com.ducnh.ibatis.cache.CacheKey;
import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.cursor.defaults.DefaultCursor;
import com.ducnh.ibatis.executor.ErrorContext;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.executor.result.DefaultResultContext;
import com.ducnh.ibatis.executor.result.DefaultResultHandler;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.ParameterMode;
import com.ducnh.ibatis.mapping.ResultMap;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.reflection.ReflectorFactory;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.session.AutoMappingBehavior;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultContext;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.ObjectTypeHandler;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.TypeHandlerRegistry;

public class DefaultResultSetHandler implements ResultSetHandler{

	private static final Object DEFERRED = new Object();
	
	private final Executor executor;
	private final Configuration configuration;
	private final MappedStatement mappedStatement;
	private final RowBounds rowBounds;
	private final ParameterHandler parameterHandler;
	private final ResultHandler<?> resultHandler;
	private final BoundSql boundSql;
	private final TypeHandlerRegistry typeHandlerRegistry;
	private final ObjectFactory objectFactory;
	private final ReflectorFactory reflectorFactory;
	
	// pending creations property tracker
	private final Map<Object, PendingRelation> pendingPccRelations = new IdentityHashMap<>();
	
	// nested resultMaps
	private final Map<CacheKey, Object> nestedResultObjects = new HashMap<>();
	private final Map<String, Object> ancestorObjects = new HashMap<>();
	private Object previousRowValue;

	// multiple resultSets
	private final Map<String, ResultMapping> nextResultMaps = new HashMap<>();
	private final Map<CacheKey, List<PendingRelation>> pendingRelations = new HashMap<>();
	
	// Cache AutoMappings
	private final Map<String, List<UnMappedColumnAutoMapping>> autoMappingsCache = new HashMap<>();
	private final Map<String, List<String>> constructorAutoMappingColumns = new HashMap<>();
	
	// temporary marking flag that indicate using constructor mapping (use field to reduce memory usage)
	private boolean useConstructorMappings;
	
	private static class PendingRelation {
		public MetaObject metaObject;
		public ResultMapping propertyMapping;
	}
	
	private static class UnMappedColumnAutoMapping {
		private final String column;
		private final String property;
		private final TypeHandler<?> typeHandler;
		private final boolean primitive;
		
		public UnMappedColumnAutoMapping(String column, String property, TypeHandler<?> typeHandler, boolean primitive) {
			this.column = column;
			this.property = property;
			this.typeHandler = typeHandler;
			this.primitive = primitive;
		}
	}
	
	public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ParameterHandler parameterHandler,
		ResultHandler<?> resultHandler, BoundSql boundSql, RowBounds rowBounds) {
		this.executor = executor;
		this.configuration = mappedStatement.getConfiguration();
		this.mappedStatement = mappedStatement;
		this.rowBounds = rowBounds;
		this.parameterHandler = parameterHandler;
		this.boundSql = boundSql;
		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.objectFactory = configuration.getObjectFactory();
		this.reflectorFactory = configuration.getReflectorFactory();
		this.resultHandler = resultHandler;
	}
	
	@Override
	public void handleOutputParameter(CallableStatement cs) throws SQLException {
		final Object parameterObject = parameterHandler.getParameterObject();
		final MetaObject metaParam = configuration.newMetaObject(parameterObject);
		final List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		for (int i = 0; i < parameterMappings.size(); i++) {
			final ParameterMapping parameterMapping = parameterMappings.get(i);
			if (parameterMapping.getMode() == ParameterMode.OUT || parameterMapping.getMode() == ParameterMode.INOUT) {
				if (ResultSet.class.equals(parameterMapping.getJavaType())) {
					handleRefCursorOutputParameter((ResultSet) cs.getObject(i + 1), parameterMapping, metaParam);
				} else {
					final String property = parameterMapping.getProperty();
					TypeHandler<?> typeHandler = parameterMapping.getTypeHandler();
					if (typeHandler == null) {
						Type javaType = parameterMapping.getJavaType();
						if (javaType == null || javaType == Object.class) {
							javaType = metaParam.getGenericSetterType(property).getKey();
						}
						JdbcType jdbcType = parameterMapping.getJdbcType();
						typeHandler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType, null);
						if (typeHandler == null) {
							typeHandler = typeHandlerRegistry.getTypeHandler(jdbcType);
							if (typeHandler == null) {
								typeHandler = ObjectTypeHandler.INSTANCE;
							}
						}
					}
					metaParam.setValue(property, typeHandler.getResult(cs, i + 1));
				}
			}
		}
	}
	
	private void handleRefCursorOutputParameter(ResultSet rs, ParameterMapping parameterMapping, MetaObject metaParam) 
		throws SQLException{
		if (rs == null) {
			return;
		}
		try {
			final String resultMapId = parameterMapping.getResultMapId();
			final ResultMap resultMap = configuration.getResultMap(resultMapId);
			final ResultSetWrapper rsw = new ResultSetWrapper(rs, configuration);
			if (this.resultHandler == null) {
				final DefaultResultHandler resultHandler = new DefaultResultHandler(objectFactory);
				handleRowValues(rsw, resultMap, resultHandler, new RowBounds(), null);
				metaParam.setValue(parameterMapping.getProperty(), resultHandler.getResultList());
			} else {
				handleRowValues(rsw, resultMap, resultHandler, new RowBounds(), null);
			}
		} finally {
			closeResultSet(rs);
		}
	}
	
	@Override
	public List<Object> handleResultSets(Statement stmt) throws SQLException {
		ErrorContext.instance().activity("handling results").object(mappedStatement.getId());
		
		final List<Object> multipleResults = new ArrayList<>();
		
		int resultSetCount = 0;
		ResultSetWrapper rsw = getFirstResultSet(stmt);
		
		List<ResultMap> resultMaps = mappedStatement.getResultMaps();
		int resultMapCount = resultMaps.size();
		validateResultMapCount(rsw, resultMapCount);
		while (rsw != null && resultMapCount > resultSetCount) {
			ResultMap resultMap = resultMaps.get(resultSetCount);
			handleResultSet(rsw, resultMap, multipleResults, null);
			rsw = getNextResultSet(stmt);
			cleanUpAfterHandlingResultSet();
			resultSetCount++;
		}
		
		String[] resultSets = mappedStatement.getResultSets();
		if (resultSets != null) {
			while (rsw != null && resultSetCount < resultSets.length) {
				ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
				if (parentMapping != null) {
					String nestedResultMapId = parentMapping.getNestedResultMapId();
					ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
					handleResultSet(rsw, resultMap, null, parentMapping);
				}
				rsw = getNextResultSet(stmt);
				cleanUpAfterHandlingResultSet();
				resultSetCount++;
			}
		}
		
		return collapseSingleResultList(multipleResults);
	}
	
	@Override 
	public <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException {
		ErrorContext.instance().activity("handling cursor results").object(mappedStatement.getId());
		
		ResultSetWrapper rsw = getFirstResultSet(stmt);
		
		List<ResultMap> resultMaps = mappedStatement.getResultMaps();
		
		int resultMapCount = resultMaps.size();
		validateResultMapsCount(rsw, resultMapCount);
		if (resultMapCount != 1) {
			throw new ExecutorException("Cursor results cannot be mapped to multiple resultMaps");
		}
		
		ResultMap resultMap = resultMaps.get(0);
		return new DefaultCursor<>(this, resultMap, rsw, rowBounds);
	}
	
	private ResultSetWrapper getFirstResultSet(Statement stmt) throws SQLException {
		ResultSet rs = null;
		SQLException e1 = null;
		
		try {
			rs = stmt.getResultSet();
		} catch (SQLException e) {
			e1 = e;
		}
		
		try {
			while (rs == null) {
				if (stmt.getMoreResults()) {
					rs = stmt.getResultSet();
				} else  if (stmt.getUpdateCount() == -1) {
					break;
				}
			}
		} catch (SQLException e) {
			throw e1 != null ? e1 : e;
		}
		
		return rs != null ? new ResultSetWrapper(rs, configuration) : null;
	}
	
	private ResultSetWrapper getNextResultSet(Statement stmt) {
		// Making this method tolerant of bad JDBC drivers
		try {
			// We stopped checking DatabaseMetaData#supportsMultipleResultSets()
			// because Oracle driver returns false
			
			if (!(!stmt.getMoreResults() && stmt.getUpdateCount() == -1)) {
				ResultSet rs = stmt.getResultSet();
				if (rs == null) {
					return getNextResultSet(stmt);
				} else {
					return new ResultSetWrapper(rs, configuration);
				}
			}
		} catch (Exception e) {
			
		}
		return null;
	}
	
	private void closeResultSet(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e ) {
			//ignore
		}
	}
	
	private void cleanUpAfterHandlingResultSet() {
		nestedResultObjects.clear();
	}
	
	private void validateResultMapsCount(ResultSetWrapper rsw, int resultMapCount) {
		if (rsw != null && resultMapCount < 1) {
			throw new ExecutorException(
				"A query was run and no Result Maps were found for the Mapped Statement '" + mappedStatement.getId()
					+ "'. 'resultType' or 'resultMap' must be specified when there is no corresponding method.");
		}
	}
	
	private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults,
		ResultMapping parentMapping) throws SQLException {
		try {
			if (parentMapping != null) {
				handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping);
			} else if (resultHandler == null) {
				DefaultResultHandler defautlResultHandler = new DefaultResultHandler(objectFactory);
				handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
				multipleResults.add(defautlResultHandler.getResultList());
			} else {
				handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
			}
		} finally {
			closeResultSet(rsw.getResultSet());
		}
	} 

	@SuppressWarnings("unchecked")
	private List<Object> collapseSingleResultList(List<Object> multipleResults) {
		return multipleResults.size() == 1 ? (List<Object>) multipleResults.get(0) : multipleResults;
	}
	
	public void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler, 
		RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
		if (resultMap.hasNestedResultMaps()) {
			ensureNoRowBounds();
			checkResultHandler();
			handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
		} else {
			handleRowValuesForSimpleResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
		}
	}
	
	private void ensureNoRowBounds() {
		if (configuration.isSafeRowBoundsEnabled() && rowBounds != null
			&& (rowBounds.getLimit() < RowBounds.NO_ROW_LIMIT || rowBounds.getOffset() > RowBounds.NO_ROW_OFFSET)) {
			throw new ExecutorException(
				"Mapped Statements with nested result mappings cannot be safely constrained by RowBounds. "
					+ "Use safeRowBoundsEnabled=false setting to bypass this check.");
		}
	}
	
	protected void checkResultHandler() {
		if (resultHandler != null && configuration.isSafeResultHandlerEnabled() && !mappedStatement.isResultOrdered()) {
			throw new ExecutorException(
				"Mapped Statements with nested result mappings cannot be safely used with a custom ResultHandler. "
					+ "Use safeResultHandlerEnabled=false setting to bypass this check "
					+ "or ensure your statement returns ordered data and set resultOrdered=true on it.");
		}
	}
	
	private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap, 
		ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
		final boolean useCollectionConstructorInjection = resultMap.hasResultMapsUsingConstructorCollection();
	
		DefaultResultContext<Object> resultContext = new DefaultResultContext<>();
		ResultSet resultSet = rsw.getResultSet();
		skipRows(resultSet, rowBounds);
		while (shouldProcessMoreRows(resultContext, rowBounds) && !resultSet.isClosed() && resultSet.next()) {
			ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(rsw, resultMap, null);
			Object rowValue = getRowValue(rsw, discriminatedResultMap, null, null);
			if (!uesCollectionConstructorInjection) {
				storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
			} else {
				if (!(rowValue instanceof PendingConstructorCreation)) {
					throw new ExecutorException("Expected result object to be a pending constructor creation!");
				}
				createAndStorePendingCreation(resultHandler, resultSet, resultContext, (PendingConstructorCreation) rowValue);
			}
		}
	}
	
	private void storeObject(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext, Object rowValue,
		ResultMapping parentMapping, ResultSet rs) throws SQLException {
		if (parentMapping != null) {
			linkToParents(rs, parentMapping, rowValue);
			return;
		}
		
		if (pendingPccRelations.containsKey(rowValue)) {
			createPendingConstructorCreations(rowValue);
		}
		
		callResultHandler(resultHandler, resultContext, rowValue);
	}
	
	@SuppressWarnings("unchecked")
	private void callResultHandler(ResultHandler<?> resultHandler, DefaultResultContext<Object> resultContext,
		Object rowValue) {
		resultContext.nextResultObject(rowValue);
		((ResultHandler<Object>) resultHandler).handleResult(resultContext);
	}
	
	private boolean shouldProcessMoreRows(ResultContext<?> context, RowBounds rowBounds) {
		return context.isStopped() && context.getResultCount() < rowBounds.getLimit();
	}
	
	private void skipRows(ResultSet rs, RowBounds rowBounds) throws SQLException {
		if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
			if (rowBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
				rs.absolute(rowBounds.getOffset());
			}
		} else {
			for (int i = 0; i < rowBounds.getOffset(); i++) {
				if (!rs.next()) {
					break;
				}
			}
		}
	}
	
	private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix, CacheKey parentRowKey) 
		throws SQLException {
		final ResultLoaderMap lazyLoader = new ResultLoaderMap();
		Object rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix, parentRowKey);
		if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
			final MetaObject metaObject = configuration.newMetaObject(rowValue);
			boolean foundValues = this.useConstructorMappings;
			if (shouldApplyAutomaticMappings(resultMap, false)) {
				foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
			}
			foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
			foundValues = lazyLoader.size() > 0 || foundValues;
			rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
		}
		
		if (parentRowKey != null) {
			// found a simple object/primitive in pending constructor creation that will need linking later
			final CacheKey rowKey = createRowKey(resultMap, rsw, columnPrefix);
			final CacheKey combineKey = combineKeys(rowKey, parentRowKey);
			
			if (combineKey != CacheKey.NULL_CACHE_KEY) {
				nestedResultObjects.put(combineKey, rowValue);s
			}
		}
		
		return rowValue;
	}
	
	private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, CacheKey combinedKey, String columnPrefix,
		Object partialObject) throws SQLException {
		final String resultMapId = resultMap.getId();
		Object rowValue = partialObject;
		if (rowValue != null) {
			final MetaObject metaObject = configuration.newMetaObject(rowValue);
			putAncestor(rowValue, resultMapId);
			applyNestedResultMappings(rsw, resultMap, metaObject, columnPrefix, combinedKey, false);
			ancestorObjects.remove(resultMapId);
		} else {
			final ResultLoaderMap lazyLoader = new ResultLoaderMap();
			rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix, combinedKey);
			if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
				final MetaObject metaObject = configuration.newMetaObject(rowValue);
				boolean foundValues = this.useConstructorMappings;
				if (shouldApplyAutomaticMappings(resultMap, true)) {
					foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
				}
				foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
				putAncestor(rowValue, resultMapId);
				foundValues = applyNestedResultMappings(rse, resultMap, metaObject, columnPrefix, combinedKey, true) || foundValues;
				ancestorObjects.remove(resultMapId);
				foundValues = lazyLoader.size() > 0 || foundValues;
				rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
			}
			if (combinedKey != CacheKey.NULL_CACHE_KEY) {
				nestedResultObjects.put(combinedKey, rowValue);
			}
		}
		return rowValue;
	}
	
	private void putAncestor(Object resultObject, String resultMapId) {
		ancestorObjects.put(resultMapId, resultObject);
	}
	
	private boolean shouldApplyAutomaticMappings(ResultMap resultMap, boolean isNested) {
		if (resultMap.getAutoMapping() != null) {
			return resultMap.getAutoMapping();
		}
		if (isNested) {
			return AutoMappingBehavior.FULL == configuration.getAutoMappingBehavior();
		} else {
			return AutoMappingBehavior.NONE != configuration.getAutoMappingBehavior();
		}
	}
	
	private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
		ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
		final Set<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
		boolean foundValues = false;
		final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
		for (ResultMapping propertyMapping : propertyMappings) {
			String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
			if (propertyMapping.getNestedResultMapId() != null && !JdbcType.CURSOR.equals(propertyMapping.getJdbcType())) {
				// the user added a column attribute to a nested result map, ignore it
				column = null;
			}
			if (propertyMapping.isCompositeResult()
				|| column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))
				|| propertyMapping.getResultSet() != null) {
				Object value = getPropertyMappingValue(rsw, metaObject, propertyMapping, lazyLoader, columnPrefix);
				final String property = propertyMapping.getProperty();
				if (property == null) {
					continue;
				}
				if (value == DEFERRED) {
					foundValues = true;
					continue;
				}
				if (value != null) {
					foundValues = true;
				}
				if (value != null
					|| configuration.isCallSetterOnNulls() && !metaObject.getSetterType(property).isPrimitive()) {
					metaObject.setValue(property, value);
				}
			}
		}
		return foundValues;
	}
	
	private Object getPropertyMappingValue(ResultSetWrapper rsw, MetaObject metaResultObject,
		ResultMapping propertyMapping, ResultLoaderMap lazyLoader, String columnPrefix) throws SQLException {
		final ResultSet rs = rsw.getResultSet();
		if (propertyMapping.getNestedQueryId() != null) {
			return getNestedQueryMappingValue(rsw, metaResultObject, propertyMapping, lazyLoader, columnPrefix);
		}
		if (JdbcType.CURSOR.equals(propertyMapping.getJdbcType())) {
			List<Object> results = getNestedCursorValue(rsw, propertyMapping, columnPrefix);
			linkObjects(metaResultObject, propertyMapping, results.get(0), true);
			return metaResultObject.getValue(propertyMapping.getProperty());
		}
		if (propertyMapping.getResultSet() != null) {
			addPendingChildRelation(rs, metaResultObject, propertyMapping);
			return DEFERRED;
		} else {
			final String column = prependPrefix(propertyMapping.getColumn(), columnPrefix);
			TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
			if (typeHandler == null) {
				final String property = propertyMapping.getProperty();
				final Type javaType = property == null ? null : metaResultObject.getGenericSetterType(property).getKey();
				typeHandler = rsw.getTypeHandler(javaType, column);
				if (typeHandler == null) {
					throw new ExecutorException(
						"No type handler found for '" + javaType + "' and JDBC type '" + rsw.getJdbcType(column) + "'"); 
				}
			}
			return typeHandler.getResult(rs, column);
		}
	}
	
	private List<Object> getNestedCursorValue(ResultSetWrapper rsw, ResultMapping propertyMapping,
		String parentColumnPrefix) throws SQLException {
		final String column = prependPrefix(propertyMapping.getColumn(), parentColumnPrefix);
		ResultMap nestedResultMap = resolveDiscriminatedResultMap(rsw, 
			configuration.getResultMap(propertyMapping.getNestedResultMapId()),
			getColumnPrefix(parentColumnPrefix, propertyMapping));
		ResultSetWrapper nestedRsw = new ResultSetWrapper(rsw.getResultSet().getObject(column, ResultSet.class),
			configuration);
		List<Object> results = new ArrayList<>();
		handleResultSet(nestedRsw, nestedResultMap, results, null);
		return results;
	}
	
	private List<UnMappedColumnAutoMapping> createAutoMappings(ResultSetWrapper rsw, ResultMap resultMap,
		MetaObject metaObject, String columnPrefix) throws SQLException {
		final String mapKey = resultMap.getId() + ":" + columnPrefix;
		List<UnMappedColumnAutoMapping> autoMapping = autoMappingsCache.get(mapKey);
		if (autoMapping == null) {
			autoMapping = new ArrayList<>();
			final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);
			List<String> mappedInConstructorAutoMapping = constructorAutoMappingColumns.remove(mapKey);
			if (mappedInConstructorAutoMapping != null) {
				unmappedColumnNames.removeAll(mappedInConstructorAutoMapping);
			}
			for (String columnName : unmappedColumnNames) {
				String propertyName = columnName;
				if (columnPrefix != null && !columnPrefix.isEmpty()) {
					if (!columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
						continue;
					}
					propertyName = columnName.substring(columnPrefix.length());
				}
				final String property = metaObject.findProperty(propertyName, configuration.isMapUnderscoreToCamelCase);
				if (property != null && metaObject.hasSetter(property)) {
					if (resultMap.getMappedProperties().contains(property)) {
						continue;
					}
					final Type propertyType = metaObject.getGenericSetterType(property).getKey();
					TypeHandler<?> typeHandler = rsw.getTypeHandler(propertyType, columnName);
					if (typeHandler != null) {
						autoMapping.add(new UnMappedColumnAutoMapping(columnName, property, typeHandler,
							propertyType instanceof Class && ((Class<?>) propertyType).isPrimitive()));
					} else {
						configuration.getAutoMappingUnknownColumnBehavior().doAction(mappedStatement, columnName, property,
							propertyType);
					}
				} else {
					configuration.getAutoMappingUnknownColumnBehavior().doAction(mappedStatement, columnName,
						property != null ? property : propertyName, null);
				}
 			}
			autoMappingsCache.put(mapKey, autoMapping);
		}
		return autoMapping;
	}
	
	private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
		String columnPrefix) throws SQLException {
		List<UnMappedColumnAutoMapping> autoMapping = createAutomaticMappings(rsw, resultMap, metaObject, columnPrefix);
		boolean foundValues = false;
		if (!autoMapping.isEmpty()) {
			for (UnMappedColumnAutoMapping mapping : autoMapping) {
				final Object value = mapping.typeHandler.getResult(rsw.getResultSet(), mapping.column);
				if (value != null) {
					foundValues = true;
				}
				if (value != null || configuration.isCallSettersOnNulls() && !mapping.primitive) {
					metaObject.setValue(mapping.property, value);
				}
			}
		}
		return foundValues;
	}
	
	private void linkToParents(ResultSet rs, ResultMapping parentMapping, Object rowValue) throws SQLException {
		CacheKey parentKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn()
			, parentMapping.getForeignColumn());
		List<PendingRelation> parents = pendingRelations.get(parentKey);
		if (parents != null) {
			for (PendingRelation parent : parents) {
				if (parent != null && rowValue != null) {
					linkObjects(parent.metaObject, parent.propertyMapping, rowValue);
				}
			}
		}
	}
	
	private void addPendingChildRelation(ResultSet rs, MetaObject metaResultObject, ResultMapping parentMapping) 
		throws SQLException {
		CacheKey cacheKey = createKeyForMultipleResults(rs, parentMapping, parentMapping.getColumn(),
			parentMapping.getColumn());
		PendingRelation deferLoad = new PendingRelation();
		deferLoad.metaObject = metaResultObject;
		deferLoad.propertyMapping = parentMapping;
		List<PendingRelation> relations = pendingRelations.computeIfAbsent(cacheKey, k -> new ArrayList<>());
		relations.add(deferLoad);
		ResultMapping previous = nextResultMaps.get(parentMapping.getResultSet());
		if (previous == null) {
			nextResultMaps.put(parentMapping.getResultSet(), parentMapping);	
		} else if (!previous.equals(parentMapping)) {
			throw new ExecutorException("Two different properties are mapped to the same resultSet");
		}
	}
	
	private CacheKey createKeyForMultipleResults(ResultSet rs, ResultMapping resultMapping, String names, String columns) 
		throws SQLException {
		CacheKey cacheKey = new CacheKey();
		cacheKey.update(resultMapping);
		if (columns != null && names != null) {
			String[] columnsArray = columns.split(",");
			String[] namesArray = names.split(",");
			for (int i = 0; i < columnsArray.length; i++) {
				Object value = rs.getString(columnsArray[i]);
				if (value != null) {
					cacheKey.update(namesArray[i]);
					cacheKey.update(value);
				}
			}
		}
		return cacheKey;
 	}
	
	private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, ResultLoaderMap lazyLoader,
		String columnPrefix, CacheKey parentRowKey) throws SQLException {
		this.useConstructorMappings = false;
		final List<Class<?>> constructorArgTypes = new ArrayList<>();
		final List<Object> constructorArgs = new ArrayList<>();
		
		Object resultObject = createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix,
			parentRowKey);
		if (resultObject != null && !hasTypeHandlerForResultObject(rws, resultMap.getType())) {
			final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
			for (ResultMapping propertyMapping : propertyMappings) {
				if (propertyMapping.getNestedQueryId() != null && propertyMapping.isLazy()) {
					resultObject = configuration.getProxyFactory().createProxy(resultObject, lazyLoader, configuration,
						objectFactory, constructroArgTypes, constructorArgs);
					break;
				}
			}
			if (resultMap.hasResultMapsUsingConstructorCollection() && resultObject instanceof PendingConstructorCreation) {
				linkNestedPendingCreations(rsw, resultMap, columnPrefix, parentRowKey,
					(PendingConstructorCreation) resultObject, constructorArgs);
			}
		}
		this.useConstructorMappings = resultObject != null && !constructorArgTypes.isEmpty();
		return resultObject;
	}
	
	
} 
