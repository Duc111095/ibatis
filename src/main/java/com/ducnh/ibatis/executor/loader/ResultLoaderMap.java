package com.ducnh.ibatis.executor.loader;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.executor.BaseExecutor;
import com.ducnh.ibatis.executor.BatchResult;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;

public class ResultLoaderMap {
	
	private final Map<String, LoadPair> loaderMap = new HashMap<>();
	
	public void addLoader(String property, MetaObject metaResultObject, ResultLoader resultLoader) {
		String upperFirst = getUppercaseFirstProperty(property);
		if (!upperFirst.equalsIgnoreCase(property) && loaderMap.containsKey(upperFirst)) {
			throw new ExecutorException("Nested lazy loader result property '" + property + "' for query id "
				+ resultLoader.mappedStatement.getId()
				+ " already exists in the result map. The leftmost property of all lazy loader properties must be unique with be unique with a result map.");
		}
		loaderMap.put(upperFirst, new LoadPair(property, metaResultObject, resultLoader));
	}
	
	public final Map<String, LoadPair> getProperties() {
		return new HashMap<>(this.loaderMap);
	}
	
	public Set<String> getPropertyNames() {
		return loaderMap.keySet();
	}
	
	public int size() {
		return loaderMap.size();
	}
	
	public boolean isEmpty() {
		return loaderMap.isEmpty();
	}
	
	public boolean hasLoader(String property) {
		return loaderMap.containsKey(property.toUpperCase(Locale.ENGLISH));
	}
	
	public boolean load(String property) throws SQLException {
		LoadPair pair = loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
		if (pair != null) {
			pair.load();
			return true;
		}
		return false;
	}
	
	public void remove(String property) {
		loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
	}
	
	public void loadAll() throws SQLException {
		final Set<String> methodNameSet = loaderMap.keySet();
		String[] methodNames = methodNameSet.toArray(new String[methodNameSet.size()]);
		for (String methodName : methodNames) {
			load(methodName);
		}
	}
	
	private static String getUppercaseFirstProperty(String property) {
		String[] parts = property.split("\\.");
		return parts[0].toUpperCase(Locale.ENGLISH);
	}
	
	public static class LoadPair implements Serializable {
		
		private static final long serialVersionUID = 20130412;
		private static final String FACTORY_METHOD = "getConfiguration";
		private final transient Object serializationCheck = new Object();
		private transient MetaObject metaResultObject;
		private transient ResultLoader resultLoader;
		private transient Log log;
		private Class<?> configurationFactory;
		private final String property;
		private String mappedStatement;
		private Serializable mappedParameter;
		
		private LoadPair(final String property, MetaObject metaResultObject, ResultLoader resultLoader) {
			this.property = property;
			this.metaResultObject = metaResultObject;
			this.resultLoader = resultLoader;
			
			// Save required information only if original object can be serialized.
			if (metaResultObject != null && metaResultObject.getOriginalObject() instanceof Serializable) {
				final Object mappedStatementParameter = resultLoader.parameterObject;
				
				if (mappedStatementParameter instanceof Serializable) {
					this.mappedStatement = resultLoader.mappedStatement.getId();
					this.mappedParameter = (Serializable) mappedStatementParameter;
					
					this.configurationFactory = resultLoader.configuration.getConfigurationFactory();
				} else {
					Log log = this.getLogger();
					if (log.isDebugEnabled()) {
						log.debug("Property [" + this.property + "] of [" + metaResultObject.getOriginalObject().getClass()
							+ "] cannot be loaded " + "after deserialization. Make sure it's loader before serializing " 
							+ "forenamed object.");
					}
				}
			}
		}
		
		public void load() throws SQLException {
			if (this.metaResultObject == null) {
				throw new IllegalArgumentException("metaResultObject is null");
			}
			if (this.resultLoader == null) {
				throw new IllegalArgumentException("resultLoader is null");
			}
			
			this.load(null);
		}
		
		public void load(final Object userObject) throws SQLException {
			if (this.metaResultObject == null || this.resultLoader == null) {
				if (this.mappedParameter == null) {
					throw new ExecutorException("Property [" + this.property + "] cannot be loaded because "
						+ "required parameter of mapped statement [" + this.mappedStatement + "] is not serializable.");
				}
				
				final Configuration config = this.getConfiguration();
				final MappedStatement ms = config.getMappedStatement(this.mappedStatement);
				if (ms == null) {
					throw new ExecutorException(
						"Cannot lazy load property [" + this.property + "] of deserialized object [" + userObject.getClass()
							+ "] because configuration does not contain statement [" + this.mappedStatement + "]");
				}
				this.metaResultObject = config.newMetaObject(userObject);
				this.resultLoader = new ResultLoader(config, new ClosedExecutor(), ms, this.mappedParameter,
					metaResultObject.getSetterType(this.property), null, null);
				
			}
			
			if (this.serializationCheck == null) {
				final ResultLoader old = this.resultLoader;
				this.resultLoader = new ResultLoader(old.configuration, new ClosedExecutor(), old.mappedStatement, 
					old.parameterObject, old.targetType, old.cacheKey, old.boundSql);
			}
			
			this.metaResultObject.setValue(property, this.resultLoader.loadResult());
		}
		
		private Configuration getConfiguration() {
			if (this.configurationFactory == null) {
				throw new ExecutorException("Cannot get Configuration as configuration factory was not set.");
			}
			
			Object configurationObject;
			try {
				final Method factoryMethod = this.configurationFactory.getDeclaredMethod(FACTORY_METHOD);
				if (!Modifier.isStatic(factoryMethod.getModifiers())) {
					throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
						+ FACTORY_METHOD + "] is not static.");
				}
				
				if (!factoryMethod.isAccessible()) {
					configurationObject = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
						try {
							factoryMethod.setAccessible(true);
							return factoryMethod.invoke(null);
						} finally {
							factoryMethod.setAccessible(false);
						}
					});
				} else {
					configurationObject = factoryMethod.invoke(null);
				}
			} catch (final ExecutorException ex) {
				throw ex;
			} catch (final NoSuchMethodException ex) {
				throw new ExecutorException("Cannot get Configuration as factory class [" + this.configurationFactory
					+ "] is missing factory method of name [" + FACTORY_METHOD + "].", ex);
			} catch (final PrivilegedActionException ex) {
				throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
					+ FACTORY_METHOD + "] threw an exception.", ex.getCause());			
			} catch (final Exception ex) {
				throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
					+ FACTORY_METHOD + "] threw an exception.", ex.getCause());
			}
			
			if (!(configurationObject instanceof Configuration)) {
				throw new ExecutorException("Cannot get Configuration as factory method [" + this.configurationFactory + "]#["
					+ FACTORY_METHOD + "] didn't return [" + Configuration.class + "] but ["
					+ (configurationObject == null ? "null" : configurationObject.getClass()) + "].");
			}
			
			return Configuration.class.cast(configurationObject);
		}
		
		private Log getLogger() {
			if (this.log == null) {
				this.log = LogFactory.getLog(this.getClass());
			}
			return this.log;
		}
	}
	
	private static final class ClosedExecutor extends BaseExecutor {
		public ClosedExecutor() {
			super(null, null);
		}
		
		@Override
		public boolean isClosed() {
			return true;
		}
		
		@Override
		protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}
		
		@Override
		public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}
		
		@Override
		protected <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds,
			ResultHandler<?> resultHandler, BoundSql boundSql) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}
		
		@Override
		protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds,
			BoundSql boundSql) throws SQLException {
			throw new UnsupportedOperationException("Not supported.");
		}
 	}
}
