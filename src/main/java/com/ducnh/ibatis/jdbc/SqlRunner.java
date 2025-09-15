package com.ducnh.ibatis.jdbc;

import java.sql.Connection;

import com.ducnh.ibatis.type.TypeHandlerRegistry;

public class SqlRunner {
	public static final int NO_GENERATED_KEY = Integer.MIN_VALUE + 1001;
	
	private final Connection connection;
	private final TypeHandlerRegistry typeHandlerRegistry;
	private boolean useGeneratedKeySupport;
}
