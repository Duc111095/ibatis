package com.ducnh.ibatis.executor;

import static com.ducnh.ibatis.executor.ExecutionPlaceholder.EXECUTION_PLACEHOLDER;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ducnh.ibatis.cache.CacheKey;
import com.ducnh.ibatis.cache.impl.PerpetualCache;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.transaction.Transaction;

public class BaseExecutor implements Executor{
	
	private static final Log log = LogFactory.getLog(BaseExecutor.class);
	
	protected Transaction transaction;
	protected Executor wrapper;
	
	protected ConcurrentLinkedQueue<DeferredLoad> deferredLoads;
	protected PerpetualCache localCache;
	protected PerpetualCache localOutputParameterCache;
	protected Configuration configuration;
	
	protected int queryStack;
	private boolean closed;
	
	protected BaseExecutor(Configuration configuration, Transaction transaction) {
		this.transaction = transaction;
		this.deferredLoads = new ConcurrentLinkedQueue<>();
		this.localCache = new PerpetualCache("LocalCache");
		this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
		this.closed = false;
		this.configuration = configuration;
		this.wrapper = this;
	}
	
	@Override
	public Transaction getTransaction() {
		if (closed) {
			throw new ExecutorException("Executor was closed");
		}
		return transaction;
	}
	
	@Override
	public void close(boolean forceRollback) {
		try {
			try {
				rollback(forceRollback);
			} finally {
				if (transaction != null) {
					transaction.close();
				}
			}
		} catch (SQLException e) {
			log.warn("Unexpected exception on closing transaction. Cause: " + e);
		} finally {
			transaction = null;
			deferredLoads = null;
			localCache = null;
			localOutputParameterCache = null;
			closed = true;
		}
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public int update(MappedStatement ms, Object parameter) throws SQLException {
		ErrorContext.instance().resource(ms.getResource()).activity("executing an updater").object(ms.getId());
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		clearLocalCache();
		return doUpdate(ms, parameter);
	}
	
	@Override
	public List<BatchResult> flushStatements() throws SQLException {
		return flushStatements(false);
	} 
	
	public List<BatchResult> flushStatements(boolean isRollback) throws SQLException {
		if (closed) {
			throw new ExecutorException("Executor was closed.");
		}
		return doFlushStatements(isRollback);
	}
	
	@Override
	public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler)
		throws SQLException {
		BoundSql boundSql = ms.getBoundSql(parameter);
		CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
		return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
	}
	
	private static class DeferredLoad {
		private final MetaObject resultObject;
		private final String property;
		private final Class<?> targetType;
		private final CacheKey key;
		private final PerpetualCache localCache;
		private final ObjectFactory objectFactory;
		private final ResultExtractor resultExtractor;
	
		public DeferredLoad(MetaObject resultObject, String property, CacheKey key, PerpetualCache localCache,
			Configuration configuration, Class<?> targetType) {
			this.resultObject = resultObject;
			this.property = property;
			this.key = key;
			this.localCache = localCache;
			this.objectFactory = configuration.getObjectFactory();
			this.resultExtractor = new ResultExtractor(configuration, objectFactory);
			this.targetType = targetType;
		}
		
		public boolean canLoad() {
			return localCache.getObject(key) != null && localCache.getObject(key) != EXECUTION_PLACEHOLDER;
		}
		
		public void load() {
			@SuppressWarnings("unchecked")
			// We suppose we get back a List
			List<Object> list = (List<Object>) localCache.getObject(key);
			Object value = resultExtractor.extractObjectFromList(list, targetType);
			resultObject.setValue(property, value);;
		}
	}
}
