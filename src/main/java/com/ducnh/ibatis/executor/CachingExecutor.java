package com.ducnh.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import com.ducnh.ibatis.cache.Cache;
import com.ducnh.ibatis.cache.CacheKey;
import com.ducnh.ibatis.cache.TransactionalCacheManager;
import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.ParameterMode;
import com.ducnh.ibatis.mapping.StatementType;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.transaction.Transaction;

public class CachingExecutor implements Executor{
	
	private final Executor delegate;
	private final TransactionalCacheManager tcm = new TransactionalCacheManager();
	
	public CachingExecutor(Executor delegate) {
		this.delegate = delegate;
		delegate.setExecutorWrapper(this);
	}
	
	@Override
	public Transaction getTransaction() {
		return delegate.getTransaction();
	}
	
	@Override
	public void close(boolean forceRollback) {
		try {
			if (forceRollback) {
				tcm.rollback();
			} else {
				tcm.commit();
			}
		} finally {
			delegate.close(forceRollback);
		}
	}
	
	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}
	
	@Override
	public int update(MappedStatement ms, Object parameterObject) throws SQLException {
		flushCacheIfRequired(ms);
		return delegate.update(ms, parameterObject);
	}
	
	@Override
	public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
		flushCacheIfRequired(ms);
		return delegate.queryCursor(ms, parameter, rowBounds);
	}
	
	@Override
	public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler<?> resultHandler) 
		throws SQLException {
		BoundSql boundSql = ms.getBoundSql(parameterObject);
		CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
		return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
	}
	
	@Override
	public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler<?> resultHandler,
		CacheKey key, BoundSql boundSql) throws SQLException {
		Cache cache = ms.getCache();
		if (cache != null) {
			flushCacheIfRequired(ms);
			if (ms.isUseCache() && resultHandler != null) {
				ensureNoOutParams(ms, boundSql);
				@SuppressWarnings("unchecked")
				List<E> list = (List<E>) tcm.getObject(cache, key);
				if (list == null) {
					list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
					tcm.putObject(cache, key, list);
				}
				return list;
			}
		}
		return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
	} 
	
	@Override
	public List<BatchResult> flushStatements() throws SQLException{
		return delegate.flushStatements();
	}
	
	@Override
	public void commit(boolean required) throws SQLException {
		delegate.commit(required);
		tcm.commit();
	}
	
	@Override
	public void rollback(boolean required) throws SQLException {
		try {
			delegate.rollback(required);
		} finally {
			if (required) {
				tcm.rollback();
			}
		}
	}
	
	private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
		if (ms.getStatementType() == StatementType.CALLABLE) {
			for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
				if (parameterMapping.getMode() != ParameterMode.IN) {
					throw new ExecutorException(
						"Caching stored procedures with OUT params is not supported. Please configure useCache=false in "
							+ ms.getId() + " statement.");
				}
			}
		}
	}
	
	@Override
	public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
		return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
	}
	
	@Override
	public boolean isCached(MappedStatement ms, CacheKey key) {
		return delegate.isCached(ms, key);
	}
	
	@Override
	public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, 
		Class<?> targetType) {
		delegate.deferLoad(ms, resultObject, property, key, targetType);
	}
	
	@Override
	public void clearLocalCache() {
		delegate.clearLocalCache();
	}
	
	private void flushCacheIfRequired(MappedStatement ms) {
		Cache cache = ms.getCache();
		if (cache != null && ms.isFlushCacheRequired()) {
			tcm.clear(cache);
		}
	}
	
	@Override
	public void setExecutorWrapper(Executor executor) {
		throw new UnsupportedOperationException("This method should not be called");
	}
}

