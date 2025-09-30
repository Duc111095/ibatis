package com.ducnh.ibatis.executor.keygen;

import java.sql.Statement;
import java.util.List;

import com.ducnh.ibatis.executor.Executor;
import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.mapping.MappedStatement;
import com.ducnh.ibatis.reflection.MetaObject;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.ExecutorType;
import com.ducnh.ibatis.session.RowBounds;

public class SelectKeyGenerator implements KeyGenerator{

	public static final String SELECT_KEY_SUFFIX = "!selectKey";
	private final boolean executeBefore;
	private final MappedStatement keyStatement;

	public SelectKeyGenerator(MappedStatement keyStatement, boolean executeBefore) {
		this.executeBefore = executeBefore;
		this.keyStatement = keyStatement;
	}
	
	@Override
	public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		if (executeBefore) {
			processGeneratedKeys(executor, ms, parameter);
		}
	}
	
	@Override
	public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		if (!executeBefore) {
			processGeneratedKeys(executor, ms, parameter);
		}  
	}
	
	private void processGeneratedKeys(Executor executor, MappedStatement ms, Object parameter) {
		try {
			if (parameter != null && keyStatement != null && keyStatement.getKeyProperties() != null) {
				String[] keyProperties = keyStatement.getKeyProperties();
				final Configuration configuration = ms.getConfiguration();
				final MetaObject metaParam = configuration.newMetaObject(parameter);
				// Do not close keyExecutor
				// The transaction will be closed by parent executor.
				Executor keyExecutor = configuration.newExecutor(executor.getTransaction(), ExecutorType.SIMPLE);
				List<Object> values = keyExecutor.query(keyStatement, parameter, RowBounds.DEFAULT,	Executor.NO_RESULT_HANDLER);
				if (values.isEmpty()) {
					throw new ExecutorException("SelectKey returned no data.");
				}
				if (values.size() > 1) {
					throw new ExecutorException("SelectKey returned more than one value.");
				} else {
					MetaObject metaResult = configuration.newMetaObject(values.get(0));
					if (keyProperties.length == 1) {
						if (metaResult.hasGetter(keyProperties[0])) {
							setValue(metaParam, keyProperties[0], metaResult.getValue(keyProperties[0]));
						} else {
							setValue(metaParam, keyProperties[0], values.get(0));
						}
					} else {
						handleMultipleProperties(keyProperties, metaParam, metaResult);
					}
				}
			}
		} catch (ExecutorException e) {
			throw e;
		} catch (Exception e) {
			throw new ExecutorException("Error selecting key or setting result to parameter object. Cause: " + e, e);
		}
	}
	
	private void handleMultipleProperties(String[] keyProperties, MetaObject metaParam, MetaObject metaResult) {
		String[] keyColumns = keyStatement.getKeyColumns();
		
		if (keyColumns == null || keyColumns.length == 0) {
			// no key columns specified, just use the property names
			for (String keyProperty : keyProperties) {
				setValue(metaParam, keyProperty, metaResult.getValue(keyProperty));
			} 
		} else {
			if (keyColumns.length != keyProperties.length) {
				throw new ExecutorException(
					"If SelectKey has key columns, the number must match the number of key properties.");
			}
			for (int i = 0; i < keyProperties.length; i++) {
				setValue(metaParam, keyProperties[i], metaResult.getValue(keyColumns[i]));
			}
		}
	}
	
	private void setValue(MetaObject metaParam, String property, Object value) {
		if (!metaParam.hasSetter(property)) {
			throw new ExecutorException("No setter found for the keyProperty '" + property + "' in "
				+ metaParam.getOriginalObject().getClass().getName() + "."); 
		}
		metaParam.setValue(property, value);
	}
}
