package com.ducnh.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;


public class YearMonthTypeHandler extends BaseTypeHandler<YearMonth>{
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, YearMonth yearMonth, JdbcType jdbcType) 
		throws SQLException {
		ps.setString(i, yearMonth.toString());
	}
	
	@Override
	public YearMonth getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toYearMonth(rs.getString(columnName));
	}
	
	@Override
	public YearMonth getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toYearMonth(rs.getString(columnIndex));
	}
	
	@Override
	public YearMonth getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toYearMonth(cs.getString(columnIndex));
	}
	
	private YearMonth toYearMonth(String value) {
		return value == null ? null : YearMonth.parse(value);
	}
}
