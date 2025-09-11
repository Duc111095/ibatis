package com.ducnh.ibatis.type;

import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class NClobTypeHandler extends BaseTypeHandler<String>{
	
	public static final NClobTypeHandler INSTANCE = new NClobTypeHandler();
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) 
			throws SQLException {
		StringReader reader = new StringReader(parameter);
		ps.setNCharacterStream(i, reader, parameter.length());
	}
	
	@Override
	public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
		NClob nclob = rs.getNClob(columnName);
		return toString(nclob);
	}
	
	@Override
	public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		NClob nclob = rs.getNClob(columnIndex);
		return toString(nclob);
	}
	
	@Override
	public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		NClob nclob = cs.getNClob(columnIndex);
		return toString(nclob);
	}
	
	private String toString(NClob nclob) throws SQLException {
		return nclob == null ? null : nclob.getSubString(1, (int) nclob.length());
	}
} 
