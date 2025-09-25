package com.ducnh.ibatis.executor.result;

import java.util.ArrayList;
import java.util.List;

import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.session.ResultContext;
import com.ducnh.ibatis.session.ResultHandler;

public class DefaultResultHandler implements ResultHandler<Object>{
	
	private final List<Object> list;
	
	public DefaultResultHandler() {
		list = new ArrayList<>();
	}
	
	@SuppressWarnings("unchecked")
	public DefaultResultHandler(ObjectFactory objectFactory) {
		list = objectFactory.create(List.class);
	} 
	
	@Override
	public void handleResult(ResultContext<?> resultContext) {
		list.add(resultContext.getResultObject());
	}
	
	public List<Object> getResultList() {
		return list;
	}
}
