package com.ducnh.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.ducnh.ibatis.executor.ErrorContext;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.type.TypeHandlerRegistry;

public abstract class BaseStatementHandler implements StatementHandler{
	
	protected final Configuration configuration;
	protected final ObjectFactory objectFactory;
	protected final TypeHandlerRegistry typeHandlerRegistry;
	protected final ResultSetHandler resultSetHandler;
	protected final ParameterHandler parameterHandler;
	
	protected final Executor executor;
	protected final MappedStatement mappedStatement;
	protected final RowBounds rowBounds;
	
	protected BoundSql boundSql;
	
	protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject,
		RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) {
		this.configuration = mappedStatement.getConfiguration();
		this.executor = executor;
		this.mappedStatement = mappedStatement;
		this.rowBounds = rowBounds;
		
		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.objectFactory = configuration.getObjectFactory();
		
		if (boundSql == null) {
			generateKeys(parameterObject);
			boundSql = mappedStatement.getBoundSql(parameterObject);
		}
		
		this.boundSql = boundSql;
		
		this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
		this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler,
			resultHandler, boundSql);
	}
	
	@Override
	public BoundSql getBoundSql() {
		return boundSql;
	}
	
	@Override
	public ParameterHandler getParameterHandler() {
		return parameterHandler;
	}
	
	@Override
	public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
		ErrorContext.instance().sql(boundSql.getSql());
		Statement statement = null;
		try {
			statement = instantiateStatement(connection);
			setStatementTimeout(statement, transactionTimeout);
			setFetchSize(statement);
			return statement;
		} catch (SQLException e) {
			closeStatement(statement);
			throw e;
		} catch (Exception e) {
			closeStatement(statement);
			throw new ExecutorException("Error preparing statement. Cause: " + e, e);
		}
	}
	
	protected abstract Statement instantiateStatement(Connection connection) throws SQLException;
	
	protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
		Integer queryTimeout = null;
		if (mappedStatement.getTimeout() != null) {
			queryTimeout = mappedStatement.getTimeout();
		} else if (configuration.getDefaultStatementTimeout() != null) {
			queryTimeout = configuration.getDefaultStatementTimeout();
		}
		if (queryTimeout != null) {
			stmt.setQueryTimeout(queryTimeout);
		}
		StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
	}
	
	protected void closeStatement(Statement statement) {
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			// ignore
		}
	}
	
	protected void generateKeys(Object parameter) {
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		ErrorContext.instance().store();
		keyGenerator.processBefore(executor, mappedStatement, null, parameter);
		ErrorContext.instance().recall();
	}
}

