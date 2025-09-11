package com.ducnh.ibatis.type;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.chrono.JapaneseDate;


public class JapaneseDateTypeHandler extends BaseTypeHandler<JapaneseDate>{

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, JapaneseDate parameter, JdbcType jdbcType) 
		throws SQLException{
		ps.setDate(i, Date.valueOf(LocalDate.ofEpochDay(parameter.toEpochDay())));
	}
	
	@Override
	public JapaneseDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Date date = rs.getDate(columnName);
		return getJapaneseDate(date);
	}
	
	@Override
	public JapaneseDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Date date = rs.getDate(columnIndex);
		return getJapaneseDate(date);
	}
	
	@Override
	public JapaneseDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Date date = cs.getDate(columnIndex);
		return getJapaneseDate(date);
	}
	
	private static JapaneseDate getJapaneseDate(Date date) {
		if (date != null) {
			return JapaneseDate.from(date.toLocalDate());
		}
		return null;
	}
}
