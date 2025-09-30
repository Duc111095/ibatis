package com.ducnh.ibatis.executor.keygen;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ducnh.ibatis.binding.MapperMethod.ParamMap;
import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.reflection.ArrayUtil;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.Configuration.StrictMap;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeHandler;
import com.ducnh.ibatis.type.TypeHandlerRegistry;

public class Jdbc3KeyGenerator implements KeyGenerator{

	private static final String SECOND_GENERIC_PARAM_NAME = ParamNameResolver.GENERIC_NAME_PREFIX + "2";
	
	public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();
	
	private static final String MSG_TOO_MANY_KEYS = "Too many keys are generated. There are only %d target objects. "
		+ "You either specified a wrong 'keyProperty' or encountered a driver bug like #1523";
	
	@Override
	public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		
	}
	
	@Override
	public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		processBatch(ms, stmt, parameter);
	}
	
	public void processBatch(MappedStatement ms, Statement stmt, Object parameter) {
		final String[] keyProperties = ms.getKeyProperties();
		if (keyProperties == null || keyProperties.length == 0) {
			return;
		}
		try (ResultSet rs = stmt.getGeneratedKeys()) {
			final ResultSetMetaData rsmd = rs.getMetaData();
			final Configuration configuration = ms.getConfiguration();
			if (rsmd.getColumnCount() < keyProperties.length) {
				
			} else {
				assignKeys(configuration, rs, rsmd, keyProperties, parameter);
			}
		} catch (Exception e) {
			throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void assignKeys(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties,
		Object parameter) throws SQLException {
		if (parameter instanceof ParamMap || parameter instanceof StrictMap) {
			assignKeysToParamMap(configuration, rs, rsmd, keyProperties, (Map<String, ?>) parameter);
		} else if (parameter instanceof ArrayList && !((ArrayList<?>) parameter).isEmpty()
			&& ((ArrayList<?>) parameter).get(0) instanceof ParamMap) {
			assignKeysToParamMapList(configuration, rs, rsmd, keyProperties, (ArrayList<ParamMap<?>>) parameter);
		} else {
			assignKeysToParam(configuration, rs, rsmd, keyProperties, parameter);
		}
	} 
	
	private void assignKeysToParam(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, 
		String[] keyProperties, Object parameter) throws SQLException {
		Collection<?> params = collectionize(parameter);
		if (params.isEmpty()) {
			return;
		}
		List<KeyAssigner> assignerList = new ArrayList<>();
		for (int i = 0; i < keyProperties.length; i++) {
			assignerList.add(new KeyAssigner(configuration, rsmd, i + 1, null, keyProperties[i]));
		}	
		Iterator<?> iterator = params.iterator();
		while (rs.next()) {
			if (!iterator.hasNext()) {
				throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, params.size()));
			}
			Object param = iterator.next();
			assignerList.forEach(x -> x.assign(rs, param));
		}
	}
	
	private void assignKeysToParamMapList(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, 
		String[] keyProperties, ArrayList<ParamMap<?>> paramMapList) throws SQLException {
		Iterator<ParamMap<?>> iterator = paramMapList.iterator();
		List<KeyAssigner> assignerList = new ArrayList<>();
		long counter = 0;
		while (rs.next()) {
			if (!iterator.hasNext()) {
				throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, counter));
			}
			ParamMap<?> paramMap = iterator.next();
			if (assignerList.isEmpty()) {
				for (int i = 0; i < keyProperties.length; i++) {
					assignerList
						.add(getAssignerForParamMap(configuration, rsmd, i+1, paramMap, keyProperties[i], keyProperties, false)
							.getValue());
				}
			}
			assignerList.forEach(x -> x.assign(rs, paramMap));
			counter++;
		}
	}
	
	private void assignKeysToParamMap(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd,
		String[] keyProperties, Map<String, ?> paramMap) throws SQLException {
		if (paramMap.isEmpty()) {
			return;
		}
		Map<String, Entry<Iterator<?>, List<KeyAssigner>>> assignerMap = new HashMap<>();
		for (int i = 0; i < keyProperties.length; i++) {
			Entry<String, KeyAssigner> entry = getAssignerForParamMap(configuration, rsmd, i + 1, paramMap, keyProperties[i],
				keyProperties, true);
			Entry<Iterator<?>, List<KeyAssigner>> iteratorPair = assignerMap.computeIfAbsent(entry.getKey(),
				k -> Map.entry(collectionize(paramMap.get(k)).iterator(), new ArrayList<>()));
			iteratorPair.getValue().add(entry.getValue());
		}
		long counter = 0;
		while (rs.next()) {
			for (Entry<Iterator<?>, List<KeyAssigner>> pair : assignerMap.values()) {
				if (!pair.getKey().hasNext()) {
					throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, counter));
				}
				Object param = pair.getKey().next();
				pair.getValue().forEach(x -> x.assign(rs, paramMap));
			}
			counter++;
		}
	}
	
	private Entry<String, KeyAssigner> getAssignerForParamMap(Configuration config, ResultSetMetaData rsmd,
		int columnPosition, Map<String, ?> paramMap, String keyProperty, String[] keyProperties, boolean omitParamName) {
		Set<String> keySet = paramMap.keySet();
		boolean singleParam = !keySet.contains(SECOND_GENERIC_PARAM_NAME);
		int firstDot = keyProperty.indexOf('.');
		if (firstDot == -1) {
			if (singleParam) {
				return getAssignerForSingleParam(config, rsmd, columnPosition, paramMap, keyProperty, omitParamName);
			}
			throw new ExecutorException("Could not determine which parameter to assign generated keys to. "
				+ "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id')."
				+ "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
				+ keySet);
		}
		String paramName = keyProperty.substring(0, firstDot);
		if (keySet.contains(paramName) ) {
			String argParamName = omitParamName ? null : paramName;
			String argKeyProperty = keyProperty.substring(firstDot + 1);
			return Map.entry(paramName, new KeyAssigner(config, rsmd, columnPosition, argParamName, argKeyProperty));
		}
		if (singleParam) {
			return getAssignerForSingleParam(config, rsmd, columnPosition, paramMap, keyProperty, omitParamName);
		} else {
			throw new ExecutorException("Could not find parameter '" + paramName + "'. "
				+ "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
				+ "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
				+ keySet);
		}
	}
	
	private Entry<String, KeyAssigner> getAssignerForSingleParam(Configuration config, ResultSetMetaData rsmd,
		int columnPosition, Map<String, ?> paramMap, String keyProperty, boolean omitParamName) {
		String singleParamName = nameOfSingleParam(paramMap);
		String argParamName = omitParamName ? null : singleParamName;
		return Map.entry(singleParamName, new KeyAssigner(config, rsmd, columnPosition, argParamName, keyProperty));
	}
	
	private static String nameOfSingleParam(Map<String, ?> paramMap) {
		return paramMap.keySet().iterator().next();
	}
	
	private static Collection<?> collectionize(Object param) {
		if (param instanceof Collection) {
			return (Collection<?>) param;
		}
		if (param instanceof Object[]) {
			return Arrays.asList((Object[]) param);
		} else {
			return Arrays.asList(param);
		}
		
	}
	
	private static class KeyAssigner {
		private final Configuration configuration;
		private final ResultSetMetaData rsmd;
		private final TypeHandlerRegistry typeHandlerRegistry;
		private final int columnPosition;
		private final String paramName;
		private final String propertyName;
		private TypeHandler<?> typeHandler;
	
		protected KeyAssigner(Configuration configuration, ResultSetMetaData rsmd, int columnPosition, String paramName,
			String propertyName) {
			this.configuration = configuration;
			this.rsmd = rsmd;
			this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
			this.columnPosition = columnPosition;
			this.paramName = paramName;
			this.propertyName = propertyName;
		}
		
		protected void assign(ResultSet rs, Object param) {
			if (paramName != null) {
				param = ((ParamMap<?>) param).get(paramName);
			}
			MetaObject metaParam = configuration.newMetaObject(param);
			try {
				if (typeHandler == null) {
					if (!metaParam.hasSetter(propertyName)) {
						throw new ExecutorException("No setter found for the keyProperty '" + propertyName + "' in '" 
							+ metaParam.getOriginalObject().getClass().getName() + "'.");
					}
					Type propertyType = metaParam.getGenericSetterType(propertyName).getKey();
					JdbcType jdbcType = JdbcType.forCode(rsmd.getColumnType(columnPosition));
					typeHandler = typeHandlerRegistry.getTypeHandler(propertyType, jdbcType);
					if (typeHandler == null) {
						typeHandler = typeHandlerRegistry.getTypeHandler(jdbcType);
					}
				}
				if (typeHandler == null) {
					
				} else {
					Object value = typeHandler.getResult(rs, columnPosition);
				}
			} catch (SQLException e) {
				throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
			}
		}
	}
}
