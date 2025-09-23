package com.ducnh.ibatis.executor;

import java.util.ArrayList;
import java.util.List;

import com.ducnh.ibatis.mapping.MappedStatement;

public class BatchResult {

	private final MappedStatement mappedStatement;
	private final String sql;
	private final List<Object> parameterObjects;
	
	private int[] updateCounts;
	
	public BatchResult(MappedStatement mappedStatement, String sql) {
		this.mappedStatement = mappedStatement;
		this.sql = sql;
		this.parameterObjects = new ArrayList<>();
	}
	
	public BatchResult(MappedStatement mappedStatement, String sql, Object parameterObject) {
		this(mappedStatement, sql);
		addParameterObject(parameterObject);
	}
	
	public MappedStatement getMappedStatement() {
		return mappedStatement;
	}
	
	public String getSql() {
		return sql;
	}
	
	public Object getParameterObject() {
		return parameterObjects.get(0);
	}
	
	public List<Object> getParameterObjects() {
		return parameterObjects;
	}
	
	public int[] getUpdateCounts() {
		return updateCounts;
	}

	public void setUpdateCounts(int[] updateCounts) {
		this.updateCounts = updateCounts;
	}
	
	public void addParameterObject(Object parameterObject) {
		this.parameterObjects.add(parameterObject);
	}
}
