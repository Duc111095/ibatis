package com.ducnh.ibatis.executor.statement;

import java.sql.Statement;
import java.util.List;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.ResultSetType;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.microsoft.sqlserver.jdbc.SQLServerStatementColumnEncryptionSetting;

public class SimpleStatementHandler extends BaseStatementHandler {

	public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
		RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}
	
	@Override
	public int update(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		int rows;
		if (keyGenerator instanceof Jdbc3KeyGenerator) {
			statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
			rows = statement.getUpdateCount();
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		} else if (keyGenerator instanceof SelectKeyGenerator) {
			statement.execute(sql);
			rows = statement.getUpdateCount();
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		} else {
			statement.execute(sql);
			rows = statement.getUpdateCount();
		}
		return rows;
	}
	
	@Override
	public void batch(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		statement.addBatch(sql);
	}
	
	@Override
	public <E> List<E> query(Statement statement, ResultHandler<?> resultHandler) throws SQLException {
		String sql = boundSql.getSql();
		statement.execute(sql);
		return resultSetHandler.handleResultSets(statement);
	}
	
	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		statement.execute(sql);
		return resultSetHandler.handleCursorResultSets(statement);
	}
	
	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
			return connection.createStatement();
		}
		return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
	}
	
	@Override
	public void parameterize(Statement statement) {
		
	}
}
