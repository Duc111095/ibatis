package com.ducnh.ibatis.mapping;

import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.type.JdbcType;
import com.ducnh.ibatis.type.TypeHandler;
import com.hazelcast.internal.util.ResultSet;

public class ParameterMapping {
	
	private static final Object UNSET = new Object();
	private Configuration configuration;
	
	private String property;
	private ParameterMode mode;
	private Class<?> javaType = Object.class;
	private JdbcType jdbcType;
	private Integer numericScale;
	private TypeHandler<?> typeHandler;
	private String resultMapId;
	private String jdbcTypeName;
	private String expression;
	private Object value = UNSET;
	
	private ParameterMapping() {
		
	}
	
	public static class Builder {
		private final ParameterMapping parameterMapping = new ParameterMapping();
		
		public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.typeHandler = typeHandler;
			parameterMapping.mode = ParameterMode.IN;
		}
		
		public Builder(Configuration configuration, String property, Class<?> javaType) {
			parameterMapping.configuration = configuration;
			parameterMapping.property = property;
			parameterMapping.javaType = javaType;
			parameterMapping.mode = ParameterMode.IN;
		}
		
		public Builder mode(ParameterMode mode) {
			parameterMapping.mode = mode;
			return this;
		}
		
		public Builder javaType(Class<?> javaType) {
			parameterMapping.javaType = javaType;
			return this;
		}
		
		public Builder jdbcType(JdbcType jdbcType) {
			parameterMapping.jdbcType = jdbcType;
			return this;
		}
		
		public Builder numericScale(Integer numericScale) {
			parameterMapping.numericScale = numericScale;
			return this;
		}
		
		public Builder resultMapId(String resultMapId) {
			parameterMapping.resultMapId = resultMapId;
			return this;
		}
		
		public Builder typeHandler(TypeHandler<?> typeHandler) {
			parameterMapping.typeHandler = typeHandler;
			return this;
		}
		
		public Builder jdbcTypeName(String jdbcTypeName) {
			parameterMapping.jdbcTypeName = jdbcTypeName;
			return this;
		}
		
		public Builder expression(String expression) {
			parameterMapping.expression = expression;
			return this;
		}
		
		public Builder value(Object value) {
			parameterMapping.value = value;
			return this;
		}
		
		public ParameterMapping build() {
			validate();
			return parameterMapping;
		}
		
		private void validate() {
			if (ResultSet.class.equals(parameterMapping.javaType) && parameterMapping.resultMapId == null) {
				throw new IllegalStateException("Missing resultMap in property '" + parameterMapping.property + "'. "
					+ "Parameters of type java.sql.ResultSet require a resultMap.");
			}
		}
	}
	
	public String getProperty() {
		return property;
	}
	
	public ParameterMode getMode() {
		return mode;
	}
	
	public Class<?> getJavaType() {
		return javaType;
	}
	
	public JdbcType getJdbcType() {
		return jdbcType;
	}
	
	public Integer getNumericScale() {
		return numericScale;
	}
	
	public TypeHandler<?> getTypeHandler() {
		return typeHandler;
	}
	
	public String getResultMapId() {
		return resultMapId;
	}
	
	public String getJdbcTypeName() {
		return jdbcTypeName;
	}
	
	public String getExpression() {
		return expression;
	}
	
	public Object getValue() {
		return value;
	}
	
	public boolean hasValue() {
		return value != UNSET;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ParameterMapping{");
		sb.append("property=").append(property).append('\'');
		sb.append(", mode=").append(mode);
		sb.append(", javaType=").append(javaType);
		sb.append(", jdbcType=").append(jdbcType);
		sb.append(", numericScale=").append(numericScale);
		sb.append(", resultMapId=").append(resultMapId).append('\'');
		sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
		sb.append(", expression='").append(expression).append('\'');
		sb.append(", value='").append(value).append('\'');
		sb.append('}');
		return sb.toString();
	}
} 
