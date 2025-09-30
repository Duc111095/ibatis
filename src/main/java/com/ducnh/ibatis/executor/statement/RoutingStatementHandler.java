package com.ducnh.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;

public class RoutingStatementHandler implements StatementHandler{
	
	private final StatementHandler delegate;
	
	public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
		ResultHandler<?> resultHandler, BoundSql boundSql) {
		switch (ms.getStatementType()) {
		case STATEMENT:
			delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
			break;
		case PREPARED:
			delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
			break;
		case CALLABLE:
			delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
			break;
		default:
			throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
		}
	}
	
	@Override
	public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
		return delegate.prepare(connection, transactionTimeout);
	}
	
	@Override
	public void parameterize(Statement statement) throws SQLException {
		delegate.parameterize(statement);
	}
	
	@Override
	public void batch(Statement statement) throws SQLException {
		delegate.batch(statement);
	}
	
	@Override
	public int update(Statement statement) throws SQLException {
		return delegate.update(statement);
	}
	
	@Override
	public <E> List<E> query(Statement statement, ResultHandler<?> resultHandler) throws SQLException {
		return delegate.query(statement, resultHandler);
	}
	
	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		return delegate.queryCursor(statement);
	}
	
	@Override
	public BoundSql getBoundSql() {
		return delegate.getBoundSql();
	}
	
	@Override
	public ParameterHandler getParameterHandler() {
		return delegate.getParameterHandler();
	}
}
