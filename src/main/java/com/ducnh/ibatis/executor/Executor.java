package com.ducnh.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import com.ducnh.ibatis.cache.CacheKey;
import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.transaction.Transaction;

public interface Executor {

	ResultHandler<?> NO_RESULT_HANDLER = null;
	
	int update(MappedStatement ms, Object parameter) throws SQLException;
	
	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler,
		CacheKey cacheKey, BoundSql boundSql) throws SQLException;
	
	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler)
		throws SQLException;
	
	<E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;
	
	List<BatchResult> flushStatements() throws SQLException;
	
	void commit(boolean required) throws SQLException;
	
	void rollback(boolean required) throws SQLException;
	
	CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);
	
	boolean isCached(MappedStatement ms, CacheKey key);
	
	void clearLocalCache();
	
	void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);
	
	Transaction getTransaction();
	
	void close(boolean forceRollback);
	
	boolean isClosed();
	
	void setExecutorWrapper(Executor executor);
}
