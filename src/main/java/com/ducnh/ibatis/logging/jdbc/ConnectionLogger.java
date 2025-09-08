package com.ducnh.ibatis.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.reflection.ExceptionUtil;

public final class ConnectionLogger extends BaseJdbcLogger implements InvocationHandler{
	private final Connection connection;
	
	private ConnectionLogger(Connection conn, Log statementLog, int queryStack) {
		super(statementLog, queryStack);
		this.connection = conn;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, params);
			} 
			if ("prepareStatement".equals(method.getName()) || "prepareCall".equals(method.getName())) {
				if (isDebugEnabled()) {
					debug(" Preparing: " + removeExtraWhitespace((String) params[0]), true);
				}
				PreparedStatement stmt = (PreparedStatement) method.invoke(connection, params);
				return PreparedStatementLogger.newInstance(stmt, statementLog, queryStack);
			}
			if ("createStatement".equals(method.getName())) {
				Statement stmt = (Statement) method.invoke(connection, params);
				return StatementLogger.newInstance(stmt, statementLog, queryStack);
			}
			return method.invoke(connection, params);
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}
	
	public static Connection newInstance(Connection conn, Log statementLog, int queryStack) {
		InvocationHandler handler = new ConnectionLogger(conn, statementLog, queryStack);
		ClassLoader cl = Connection.class.getClassLoader();
		return (Connection) Proxy.newProxyInstance(cl, new Class[] {Collection.class}, handler);
	}
	
	public Connection getConnection() {
		return connection;
	}
}
