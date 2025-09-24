package com.ducnh.ibatis.executor.statement;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.mapping.ParameterMapping;
import com.ducnh.ibatis.mapping.ParameterMode;
import com.ducnh.ibatis.mapping.ResultSetType;
import com.ducnh.ibatis.session.ResultHandler;
import com.ducnh.ibatis.session.RowBounds;
import com.ducnh.ibatis.type.JdbcType;

public class CallableStatementHandler extends BaseStatementHandler{

	public CallableStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter,
		RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}
	
	@Override
	public int update(Statement statement) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		cs.execute();
		int rows = cs.getUpdateCount();
		Object parameterObject = boundSql.getParameterObject();
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		keyGenerator.processAfter(executor, mappedStatement, cs, parameterObject);
		resultSetHandler.handleOutputParameter(cs);
		return rows;
	}
	
	@Override
	public void batch(Statement statement) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		cs.addBatch();
	}
	
	@Override
	public <E> List<E> query(Statement statement, ResultHandler<?> resultHandler) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		cs.execute();
		List<E> resultList = resultSetHandler.handleResultSets(cs);
		resultSetHandler.handleOutputParameters(cs);
		return resultList;
	}
	
	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		CallableStatement cs = (CallableStatement) statement;
		cs.execute();
		Cursor<E> resultList = resultSetHandler.handleCursorResultSets(cs);
		resultSetHandler.handleOutputParameters(cs);
		return resultList;
	}
	
	@Override
	public Statement instantiateStatement(Connection connection) throws SQLException {
		String sql = boundSql.getSql();
		if (mappedStatement.getResultSetType() == ResultSetType.DEFAULT) {
			return connection.prepareCall(sql);
		}
		return connection.prepareCall(sql, mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
	}
	
	@Override
	public void parameterize(Statement statement) throws SQLException {
		registerOutputParameters((CallableStatement) statement);
		parameterHandler.setParameters((CallableStatement) statement);
	}
	
	private void registerOutputParameters(CallableStatement cs) throws SQLException {
		List<ParameterMapping> parameterMappings= boundSql.getParameterMappings();
		for (int i = 0, n = parameterMappings.size(); i < n; i++) {
			ParameterMapping parameterMapping = parameterMappings.get(i);
			if (parameterMapping.getMode() == ParameterMode.OUT || parameterMapping.getMode() == ParameterMode.INOUT) {
				if (null == parameterMapping.getJdbcType()) {
					throw new ExecutorException(
						"The JDBC Type must be specified for output parameter. Parameter: " + parameterMapping.getProperty());
				}
				if (parameterMapping.getNumericScale() != null && (parameterMapping.getJdbcType() == JdbcType.NUMERIC
					|| parameterMapping.getJdbcType() == JdbcType.DECIMAL)) {
					cs.registerOutParameter(i + 1, parameterMapping.getJdbcType().TYPE_CODE, parameterMapping.getNumericScale());
				}  else {
					if (parameterMapping.getJdbcTypeName() == null) {
						cs.registerOutParameter(i + 1, parameterMapping.getJdbcType().TYPE_CODE, parameterMapping.getJdbcTypeName());
					}
				}
			}
		}
	}
}
