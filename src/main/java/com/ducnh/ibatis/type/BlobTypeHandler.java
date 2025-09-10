package com.ducnh.ibatis.type;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class BlobTypeHandler extends BaseTypeHandler<byte[]> {
	public static final BlobTypeHandler INSTANCE = new BlobTypeHandler();
	
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType) 
		throws SQLException {
		ByteArrayInputStream bis = new ByteArrayInputStream(parameter);
		ps.setBinaryStream(i, bis, parameter.length);
	}
	
	@Override
	public byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
		return toPrimitiveBytes(rs.getBlob(columnName));
	}
	
	@Override
	public byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		return toPrimitiveBytes(rs.getBlob(columnIndex));
	}
	
	@Override
	public byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		return toPrimitiveBytes(cs.getBlob(columnIndex));
	}
	
	private byte[] toPrimitiveBytes(Blob blob) throws SQLException {
		return blob == null ? null : blob.getBytes(1, (int) blob.length());
	}
}
