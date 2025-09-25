package com.ducnh.ibatis.executor.resultset;

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.ducnh.ibatis.annotations.ResultMap;
import com.ducnh.ibatis.cache.CacheKey;
import com.ducnh.ibatis.executor.ErrorContext;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.executor.result.DefaultResultHandler;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.ParameterMode;
import com.ducnh.ibatis.mapping.ResultMapping;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.reflection.ReflectorFactory;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.session.Configuration;
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
	}
} 
