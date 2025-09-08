package com.ducnh.ibatis.session;

public class SqlSessionFactoryBuilder {
	
	public SqlSessionFactory build(Reader reader) {
		return build(reader, null, null);
	}
}
