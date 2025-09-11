package com.ducnh.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.ducnh.ibatis.executor.ExecutorException;

public class ConflictedTypeHandler implements TypeHandler<Object>{
	private final Class<?> javaType;
	private final JdbcType jdbcType;
	private final Set<TypeHandler<?>> conflictedTypeHandlers = new HashSet<>();

	public ConflictedTypeHandler(Class<?> javaType, JdbcType jdbcType, TypeHandler<?> existing, TypeHandler<?> added) {
		super();
		this.javaType = javaType;
		this.jdbcType = jdbcType;
		if (existing instanceof ConflictedTypeHandler) {
			conflictedTypeHandlers.addAll(((ConflictedTypeHandler) existing).getConflictedTypeHandlers());
		} else {
			conflictedTypeHandlers.add(existing);
		}
		conflictedTypeHandlers.add(added);
	}
	
	private Set<TypeHandler<?>> getConflictedTypeHandlers() {
		return conflictedTypeHandlers;
	}
	
	@Override
	public void setParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
		throw exception();
	}
	
	@Override
	public Object getResult(ResultSet rs, String columnName) throws SQLException {
		throw exception();
	}
	
	@Override
	public Object getResult(ResultSet rs, int columnIndex) throws SQLException {
		throw exception();
	}
	
	@Override 
	public Object getResult(CallableStatement cs, int columnIndex) throws SQLException {
		throw exception();
	}
	
	private ExecutorException exception() {
		return new ExecutorException(
			"Multiple type-aware handlers are registered and being looked up without type; javaType=" + javaType
				+ ", jdbcType=" + jdbcType + ", type handlers=" 
				+ conflictedTypeHandlers.stream().map(x -> x.getClass().getName()).collect(Collectors.joining(",")));
		
	}
}
