package com.ducnh.ibatis.transaction.jdbc;


import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.session.TransactionIsolationLevel;
import com.ducnh.ibatis.transaction.Transaction;
import com.ducnh.ibatis.transaction.TransactionException;

public class JdbcTransaction implements Transaction{

	private static final Log log = LogFactory.getLog(JdbcTransaction.class);
	
	protected Connection connection;
	protected DataSource dataSource;
	protected TransactionIsolationLevel level;
	protected boolean autoCommit;
	protected boolean skipSetAutoCommitOnClose;
	
	public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit) {
		this(ds, desiredLevel, desiredAutoCommit, false);
	}
	
	public JdbcTransaction(DataSource ds, TransactionIsolationLevel desiredLevel, boolean desiredAutoCommit,
		boolean skipSetAutoCommitOnClose) {
		dataSource = ds;
		level = desiredLevel;
		autoCommit = desiredAutoCommit;
		this.skipSetAutoCommitOnClose = skipSetAutoCommitOnClose;
	}
	
	public JdbcTransaction(Connection conn) {
		this.connection = conn;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (connection == null) {
			openConnection();
		}
		return connection;
	}
	
	@Override
	public void commit() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Committing JDBC Connection [" + connection + "]");
			}
			connection.commit();
		}
	}
	
	@Override
	public void rollback() throws SQLException {
		if (connection != null && !connection.getAutoCommit()) {
			if (log.isDebugEnabled()) {
				log.debug("Rolling back JDBC Connection [" + connection + "]");
			}
			connection.rollback();
		}
	}
	
	@Override
	public void close() throws SQLException {
		if (connection != null) {
			resetAutoCommit();
			if (log.isDebugEnabled()) {
				log.debug("Closing JDBC Connection [" + connection + "]");
			}
			connection.close();
		}
	}
	
	protected void setDesiredAutoCommit(boolean desiredAutoCommit) {
		try {
			if (connection.getAutoCommit() != desiredAutoCommit) {
				if (log.isDebugEnabled()) {
					log.debug("Setting autocommit to " + desiredAutoCommit + " on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(desiredAutoCommit);
			}
		} catch (SQLException e) {
			// Only a very poorly implemented driver would fail here,
			// and there's not much we can do about that.
			throw new TransactionException(
				"Error configuring AutoCommit. " + "Your driver may not support getAutoCommit() or setAutoCommit(). "
					+ "Requested setting: " + desiredAutoCommit + ". Cause: " + e, e);
		}
	}
	
	protected void resetAutoCommit() {
		try {
			if (!skipSetAutoCommitOnClose && !connection.getAutoCommit()) {
				if (log.isDebugEnabled()) {
					log.debug("Resetting autocommit to true on JDBC Connection [" + connection + "]");
				}
				connection.setAutoCommit(true);
			}
		} catch (SQLException e) {
			if (log.isDebugEnabled()) {
				log.debug("Error resetting autocommit to true " + "before closing the connection. Cause: " + e);
			}
		}
	}
	
	protected void openConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("Opening JDBC Connection");
		}
		connection = dataSource.getConnection();
		if (level != null) {
			connection.setTransactionIsolation(level.getLevel());
		}
		setDesiredAutoCommit(autoCommit);
	}
	
	@Override
	public Integer getTimeout() throws SQLException {
		return null;
	}
}
