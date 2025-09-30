package com.ducnh.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.keygen.Jdbc3KeyGenerator;
import com.ducnh.ibatis.executor.keygen.KeyGenerator;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.ResultSetType;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;

public class PreparedStatementHandler extends BaseStatementHandler {

	public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, 
		RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}
	
	@Override
	public int update(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.execute();
		int rows = ps.getUpdateCount();
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
		return rows;
	}
	
	@Override
	public void batch(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.addBatch();
	}
	
	@Override
	public <E> List<E> query(Statement statement, ResultHandler<?> resultHandler) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.execute();
		return resultSetHandler.handleResultSets(ps);
	}
	
	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		PreparedStatement ps = (PreparedStatement) statement;
		ps.execute();
		return resultSetHandler.handleCursorResultSets(ps);
	}
	
	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		String sql = boundSql.getSql();
		if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
			String[] keyColumnNames = mappedStatement.getKeyColumns();
			if (keyColumnNames == null) {
				return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			} else {
				return connection.prepareStatement(sql, keyColumnNames);
			}
		}
		if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
			return connection.prepareStatement(sql);
		} else {
			return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(), 
				ResultSet.CONCUR_READ_ONLY);
		}
	}
	
	@Override
	public void parameterize(Statement statement) throws SQLException {
		parameterHandler.setParameters((PreparedStatement) statement);
	}
}
