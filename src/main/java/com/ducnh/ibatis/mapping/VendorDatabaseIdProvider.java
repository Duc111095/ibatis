package com.ducnh.ibatis.mapping;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.ducnh.ibatis.builder.BuilderException;

public class VendorDatabaseIdProvider implements DatabaseIdProvider{

	private Properties properties;
	
	@Override
	public String getDatabaseId(DataSource dataSource) {
		if (dataSource == null) {
			throw new NullPointerException("dataSource cannot be null");
		}
		try {
			return getDatabaseName(dataSource);
		} catch (SQLException e) {
			throw new BuilderException("Error occured when getting DB product name", e);
		}
	}
	
	@Override
	public void setProperties(Properties p) {
		this.properties = p;
	}
	
	private String getDatabaseName(DataSource dataSource) throws SQLException {
		String productName = getDatabaseProductName(dataSource);
		if (properties == null || properties.isEmpty()) {
			return productName;
		}
		return properties.entrySet().stream().filter(entry -> productName.contains((String) entry.getKey()))
				.map(entry -> (String) entry.getValue()).findFirst().orElse(null);
	}
	
	private String getDatabaseProductName(DataSource dataSource) throws SQLException {
		try (Connection conn = dataSource.getConnection()) {
			return conn.getMetaData().getDatabaseProductName();
		}
	}
}
