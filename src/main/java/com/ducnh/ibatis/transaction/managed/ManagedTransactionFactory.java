package com.ducnh.ibatis.transaction.managed;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import com.ducnh.ibatis.session.TransactionIsolationLevel;
import com.ducnh.ibatis.transaction.Transaction;
import com.ducnh.ibatis.transaction.TransactionFactory;

public class ManagedTransactionFactory implements TransactionFactory{

	private boolean closeConnection = true;
	
	@Override
	public void setProperties(Properties props) {
		if (props != null) {
			String closeConnectionProperty = props.getProperty("closeConnection");
			if (closeConnectionProperty != null) {
				closeConnection = Boolean.parseBoolean(closeConnectionProperty);
			}
		}
	}
	
	@Override
	public Transaction newTransaction(Connection conn) {
		return new ManagedTransaction(conn, closeConnection);
	}
	
	@Override
	public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
		return new ManagedTransaction(ds, level, closeConnection);
	}
}
