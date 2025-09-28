package com.ducnh.ibatis.session;


import java.lang.reflect.Type;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.mapping.MappedStatement;

public enum AutoMappingUnknownColumnBehavior {

	NONE {
		@Override
		public void doAction(MappedStatement mapppedStatement, String columnName, String property, Type propertyType) {
			// do nothing
		}
	}, 
	WARNING {
		@Override
		public void doAction(MappedStatement mappedStatement, String columnName, String property, Type propertyType ) {
			LogHolder.log.warn(buildMessage(mappedStatement, columnName, property, propertyType));
		}
	}, 
	FAILING {
		@Override
		public void doAction(MappedStatement mappedStatement, String columnName, String property, Type propertyType) {
			throw new SqlSessionException(buildMessage(mappedStatement, columnName, property, propertyType));
		}
	};
	public abstract void doAction(MappedStatement mappedStatement, String columnName, String propertyName,
		Type propertyType);
	
	private static String buildMessage(MappedStatement mappedStatement, String columnName, String property, 
		Type propertyType) {
		return new StringBuilder("Unknown column is detected on '").append(mappedStatement.getId())
			.append("' auto-mapping. Mapping parameters are ").append("[").append("columnName=").append(columnName)
			.append(",").append("propertyName=").append(property).append(".").append("propertyType=")
			.append(propertyType != null ? propertyType.getTypeName() : null).append("]").toString();
	}
	
	private static class LogHolder {
		private static final Log log = LogFactory.getLog(AutoMappingUnknownColumnBehavior.class);
	}
}
