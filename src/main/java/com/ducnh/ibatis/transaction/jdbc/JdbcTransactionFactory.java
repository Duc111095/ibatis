package com.ducnh.ibatis.transaction.jdbc;

import java.sql.Connection;
import java.util.Properties;

import com.ducnh.ibatis.transaction.Transaction;
import com.ducnh.ibatis.transaction.TransactionFactory;

public class JdbcTransactionFactory implements TransactionFactory{

	private boolean skipSetAutoCommitOnClose;
	
	@Override
	public void setProperties(Properties props) {
		if (props == null) {
			return;
		}
		String value = props.getProperty("skipSetAutoCommitOnClose");
		if (value != null) {
			skipSetAutoCommitOnClose = Boolean.parseBoolean(value);
		}
	}
	
	@Override
	public Transaction newTransaction(Connection conn) {
		return new JdbcTransaction(conn);
	}
}
