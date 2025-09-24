package com.ducnh.ibatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.transaction.Transaction;

public class SimpleExecutor extends BaseExecutor{
	
	public SimpleExecutor(Configuration configuration, Transaction transaction) {
		super(configuration, transaction);
	}
	
	@Override
	public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
		Statement stmt = null;
		try {
			Configuration configuration = ms.getConfiguration();
			StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
			stmt = preparrStatement(handler, ms.getStatementLog());
			return handler.update(stmt);
		} finally {
			closeStatement(stmt);
		}
	}

	@Override
	public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler,
		BoundSql boundSql) throws SQLException {
		Statement stmt = null;
		try {
			Configuration configuration = ms.getConfiguration();
			StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler,
				boundSql);
			stmt = prepareStatement(handler, ms.getStatementLog());
			return handler.query(stmt, resultHandler);
		} finally {
			closeStatement(stmt);
		}
	} 
	
	@Override
	protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) 
		throws SQLException {
		Configuration configuration = ms.getConfiguration();
		StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
		Statement stmt = prepareStatement(handler, ms.getStatementLog());
		Cursor<E> cursor = handler.queryCursor(stmt);
		stmt.closeOnCompletion();
		return cursor;
	}
	
	@Override
	public List<BatchResult> doFlushStatements(boolean isRollback) {
		return Collections.emptyList();
	}
	
	private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
		Statement stmt;
		Connection connection = getConnection(statementLog);
		stmt = handler.prepare(connection, transaction.getTimeout());
		handler.parameterize(stmt);
		return stmt;
	}
}
