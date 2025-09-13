package com.ducnh.ibatis.mapping;

public interface SqlSource {

	BoundSql getBoundSql(Object parameterObject);
}
