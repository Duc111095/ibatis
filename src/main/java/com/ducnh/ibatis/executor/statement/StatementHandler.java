package com.ducnh.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.ducnh.ibatis.cursor.Cursor;
import com.ducnh.ibatis.executor.parameter.ParameterHandler;
import com.ducnh.ibatis.mapping.BoundSql;
import com.ducnh.ibatis.session.ResultHandler;

public interface StatementHandler {
	
	Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException;
	
	void parameterize(Statement statement) throws SQLException;
	
	void batch(Statement statement) throws SQLException;
	
	int update(Statement statement) throws SQLException;
	
	<E> List<E> query(Statement statement, ResultHandler<?> resultHandler) throws SQLException;

	<E> Cursor<E> queryCursor(Statement statement) throws SQLException;
	
	BoundSql getBoundSql();
	
	ParameterHandler getParameterHandler();
}
