package com.ducnh.ibatis.binding;

import java.awt.Cursor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.reflection.TypeParameterResolver;
import com.ducnh.ibatis.session.Configuration;
import com.ducnh.ibatis.session.RowBounds;

public class MapperMethod {
	
	private final SqlCommand command;
	private final MethodSignature method;
	
	public static class MethodSignature {
		
		private final boolean returnsMany;
		private final boolean returnsMap;
		private final boolean returnsVoid;
		private final boolean returnsCursor;
		private final boolean returnsOptional;
		private final Class<?> returnType;
		private final String mapKey;
		private final Integer resultHandlerIndex;
		private final Integer rowBoundsIndex;
		private final ParamNameResolver paramNameResolver;
		
		public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
			Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
			if (resolvedReturnType instanceof Class<?>) {
				this.returnType = (Class<?>) resolvedReturnType;
			} else if (resolvedReturnType instanceof ParameterizedType) {
				this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
			} else {
				this.returnType = method.getReturnType();
			}
			this.returnsVoid = void.class.equals(this.returnType);
			this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
			this.returnsCursor = Cursor.class.equals(this.returnType);
			this.returnsOptional = Optional.class.equals(this.returnType);
			this.mapKey = getMapKey(method);
			this.returnsMap = this.mapKey != null;
			this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
			this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
			this.paramNameResolver = new ParamNameResolver(configuration, method, mapperInterface);
		}
		
		public Object convertArgsToSqlCommandParam(Object[] args) {
			return paramNameResolver.getNamedParams(args);
		}
	}
}
