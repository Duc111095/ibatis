package com.ducnh.ibatis.executor.resultset;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.ducnh.ibatis.cursor.Cursor;

public interface ResultSetHandler {

	<E> List<E> handleResultSets(Statement stmt) throws SQLException;
	
	<E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;
	
	void handleOutputParameters(CallableStatement cs) throws SQLException;
}
