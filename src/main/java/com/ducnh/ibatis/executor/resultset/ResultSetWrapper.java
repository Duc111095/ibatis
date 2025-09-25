package com.ducnh.ibatis.executor.resultset;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ducnh.ibatis.io.Resources;
import com.ducnh.ibatis.mapping.ResultMap;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.ObjectTypeHandler;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.TypeHandlerRegistry;

public class ResultSetWrapper {

	private final ResultSet resultSet;
	private final TypeHandlerRegistry typeHandlerRegistry;
	private final List<String> columnNames = new ArrayList<>();
	private final List<String> classNames = new ArrayList<>();
	private final List<JdbcType> jdbcTypes = new ArrayList<>();
	private final Map<String, Map<Type, TypeHandler<?>>> typeHandlerMap = new HashMap<>();
	private final Map<String, Set<String>> mappedColumnNamesMap = new HashMap<>();
	private final Map<String, List<String>> unMappedColumnNamesMap = new HashMap<>();
	
	public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {
		this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		this.resultSet = rs;
		final ResultSetMetaData metaData = rs.getMetaData();
		final int columnCount = metaData.getColumnCount();
		for (int i = 1; i < columnCount; i++) {
			columnNames.add(configuration.isUseColumnLabel() ? metaData.getColumnLabel(i) : metaData.getColumnName(i));
			jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
			classNames.add(metaData.getColumnClassName(i));
		}
	}
	
	public ResultSet getResultSet() {
		return resultSet;
	}
	
	public List<String> getColumnNames() {
		return this.columnNames;
	}
	
	public List<String> getClassNames() {
		return Collections.unmodifiableList(classNames);
	}
	
	public List<JdbcType> getJdbcTypes() {
		return jdbcTypes;
	}
	
	public JdbcType getJdbcType(String columnName) {
		int columnIndex = getColumnIndex(columnName);
		return columnIndex == -1 ? null : jdbcTypes.get(columnIndex);
	}
	
	public TypeHandler<?> getTypeHandler(Type propertyType, String columnName) {
		return typeHandlerMap.computeIfAbsent(columnName, k -> new HashMap<>()).computeIfAbsent(propertyType, k -> {
			int index = getColumnIndex(columnName);
			if (index == -1) {
				return ObjectTypeHandler.INSTANCE;
			}
			JdbcType jdbcType = jdbcTypes.get(index);
			TypeHandler<?> handler = typeHandlerRegistry.getTypeHandler(k, jdbcType, null);
			if (handler != null) {
				return handler;
			}
			
			Class<?> javaType = resolveClass(classNames.get(index));
			if (!(k instanceof Class && ((Class<?>)k).isAssignableFrom(javaType))) {
				// Cleary incompatible
				return null;
			}
			
			handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType, null);
			if (handler == null) {
				handler = typeHandlerRegistry.getTypeHandler(jdbcType);
			}
			return handler == null ? ObjectTypeHandler.INSTANCE : handler;
		});
	}
	
	static Class<?> resolveClass(String className) {
		try {
			if (className != null) {
				return Resources.classForName(className);
			}
		} catch (ClassNotFoundException e) {
			// ignore
		}
		return null;
	}
	
	private int getColumnIndex(String columnName) {
		for (int i = 0; i < columnNames.size(); i++) {
			if (columnNames.get(i).equalsIgnoreCase(columnName)) {
				return i;
			}
		}
		return -1;
	}
	
	private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
		Set<String> mappedColumnNames = new HashSet<>();
		List<String> unmappedColumnNames = new ArrayList<>();
		final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
		final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
		for (String columnName : columnNames) {
			final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
			if (mappedColumns.contains(upperColumnName)) {
				mappedColumnNames.add(upperColumnName);
			} else {
				unmappedColumnNames.add(columnName);
			}
		}
		mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
		unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
	}
	
	public Set<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
		Set<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		if (mappedColumnNames == null) {
			loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
			mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		}
		return mappedColumnNames;
	}
	
	public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
		List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		if (unMappedColumnNames == null) {
			loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
			unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
		}
		return unMappedColumnNames;
	}
	
	private String getMapKey(ResultMap resultMap, String columnPrefix) {
		return resultMap.getId() + ":" + columnPrefix;
	}
	
	private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
		if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
			return columnNames;
		}
		final Set<String> prefixed = new HashSet<>();
		for (String columnName : columnNames) {
			prefixed.add(prefix + columnName);
		}
		return prefixed;
	}	
}
