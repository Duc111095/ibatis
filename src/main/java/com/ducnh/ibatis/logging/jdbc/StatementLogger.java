package com.ducnh.ibatis.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.reflection.ExceptionUtil;

public final class StatementLogger  extends BaseJdbcLogger implements InvocationHandler{
	private final Statement statement;

	private StatementLogger(Statement stmt, Log statementLog, int queryStack) {
		super(statementLog, queryStack);
		this.statement = stmt;
	} 
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
		try {
			if (Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, params);
			}
			if (EXECUTE_METHODS.contains(method.getName())) {
				if (isDebugEnabled()) {
					debug("  Executing: " + removeExtraWhitespace((String) params[0]), true);
				}
				if ("executeQuery".equals(method.getName())) {
					ResultSet rs = (ResultSet) method.invoke(statement, params);
					return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
				} else {
					return method.invoke(statement, params);
				}
			}
			if ("getResultSet".equals(method.getName())) {
				ResultSet rs = (ResultSet) method.invoke(statement, params);
				return rs == null ? null : ResultSetLogger.newInstance(rs, statementLog, queryStack);
			} else {
				return method.invoke(statement, params);
			}
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
	}
	
	public static Statement newInstance(Statement stmt, Log statementLog, int queryStack) {
		InvocationHandler handler = new StatementLogger(stmt, statementLog, queryStack);
		ClassLoader cl = Statement.class.getClassLoader();
		return (Statement) Proxy.newProxyInstance(cl, new Class[] {Statement.class}, handler);
	}
	
	public Statement getStatement() {
		return statement;
	}
}
