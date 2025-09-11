package com.ducnh.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DateOnlyTypeHandler extends BaseTypeHandler<Date>{
	
	public static final DateOnlyTypeHandler INSTANCE = new DateOnlyTypeHandler();
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType) throws SQLException {
		ps.setDate(i, new java.sql.Date(parameter.getTime()));
	}
	
	@Override
	public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toDate(rs.getDate(columnName));
	}
	
	@Override
	public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toDate(rs.getDate(columnIndex));
	}
	
	@Override
	public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toDate(cs.getDate(columnIndex));
	}
	
	private Date toDate(java.sql.Date date) {
		return date == null ? null : new Date(date.getTime());
	}
}
