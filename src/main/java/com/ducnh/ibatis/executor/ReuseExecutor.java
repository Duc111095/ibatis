package com.ducnh.ibatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.executor.statement.StatementHandler;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.transaction.Transaction;

public class ReuseExecutor extends BaseExecutor{

	private final Map<String, Statement> statementMap = new HashMap<>();

	public ReuseExecutor(Configuration configuration, Transaction transaction) {
		super(configuration, transaction);
	}
	
	@Override
	public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
		Configuration configuration = ms.getConfiguration();
		StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
		Statement stmt = prepareStatement(handler, ms.getStatementLog());
		return handler.update(stmt);
	}
	
	@Override
	public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, 
		BoundSql boundSql) throws SQLException {
		Configuration configuration = ms.getConfiguration();
		StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler,
			boundSql);
		Statement stmt = prepareStatement(handler, ms.getStatementLog());
		return handler.query(stmt, resultHandler);
	}
	
	@Override
	protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
		throws SQLException {
		Configuration configuration = ms.getConfiguration();
		StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
		Statement stmt = prepareStatement(handler, ms.getStatementLog());
		return handler.queryCursor(stmt);
	}
	
	@Override
	public List<BatchResult> doFlushStatements(boolean isRollback) {
		for (Statement stmt : statementMap.values()) {
			closeStatement(stmt);
		}
		statementMap.clear();
		return Collections.emptyList();
	}
	
	private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
		Statement stmt;
		BoundSql boundSql = handler.getBoundSql();
		String sql = boundSql.getSql();
		if (hasStatementFor(sql) ) {
			stmt = getStatement(sql);
			applyTransactionTimeout(stmt);
		} else {
			Connection connection = getConnection(statementLog);
			stmt = handler.prepare(connection, transaction.getTimeout());
			putStatement(sql, stmt);
		}
		handler.parameterize(stmt);
		return stmt;
	}
	
	private boolean hasStatementFor(String sql) {
		try {
			Statement statement = statementMap.get(sql);
			return statement != null && !statement.getConnection().isClosed();
		} catch (SQLException e) {
			return false;
		}
	}
	
	private Statement getStatement(String s) {
		return statementMap.get(s);
	}
	
	private void putStatement(String sql, Statement stmt) {
		statementMap.put(sql, stmt);
	}
} 
