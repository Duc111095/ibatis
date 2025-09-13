package com.ducnh.ibatis.binding;

import java.lang.reflect.Method;

import com.ducnh.ibatis.session.SqlSession;

public interface MapperMethodInvoker {

	Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable;
}
