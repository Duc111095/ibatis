package com.ducnh.ibatis.transaction.managed;



import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.session.TransactionIsolationLevel;
import com.ducnh.ibatis.transaction.Transaction;

public class ManagedTransaction implements Transaction{
	
	private static final Log log = LogFactory.getLog(ManagedTransaction.class);
	
	private DataSource dataSource;
	private TransactionIsolationLevel level;
	private Connection connection;
	private final boolean closeConnection;
	
	public ManagedTransaction(Connection connection, boolean closeConnection) {
		this.connection = connection;
		this.closeConnection = closeConnection;
	}
	
	public ManagedTransaction(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
		this.dataSource = ds;
		this.level = level;
		this.closeConnection = closeConnection;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (this.connection == null) {
			openConnection();
		}
		return this.connection;
	}
	
	@Override
	public void commit() throws SQLException {
		// Does nothing
	}
	
	@Override
	public void rollback() throws SQLException {
		// Does nothing
	}
	
	@Override
	public void close() throws SQLException {
		if (this.closeConnection && this.connection != null) {
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + this.connection + "]");
			}
			this.connection.close();
		}
	}
	
	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		this.connection = this.dataSource.getConnection();
		if (this.level != null) {
			this.connection.setTransactionIsolation(this.level.getLevel());
		}
	}
	
	@Override
	public Integer getTimeout() throws SQLException {
		return null;
	}
}
