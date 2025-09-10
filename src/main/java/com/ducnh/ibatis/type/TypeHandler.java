package com.ducnh.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface TypeHandler<T> {
	
	void setParameter(PreparedStatement ps, int t, T parameter, JdbcType jdbcType) throws SQLException;
	
	T getResult(ResultSet rs, String columnName) throws SQLException;
	
	T getResult(ResultSet rs, int columnIndex) throws SQLException;
	
	T getResult(CallableStatement cs, int columnIndex) throws SQLException;
}
