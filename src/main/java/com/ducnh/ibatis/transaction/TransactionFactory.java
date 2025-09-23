package com.ducnh.ibatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import com.ducnh.ibatis.session.TransactionIsolationLevel;

public interface TransactionFactory {
	
	default void setProperties(Properties props) {
		
	}
	
	Transaction newTransaction(Connection conn);
	
	Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);
}
