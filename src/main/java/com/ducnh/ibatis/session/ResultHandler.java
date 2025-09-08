package com.ducnh.ibatis.session;

public interface ResultHandler<T> {
	
	void handleResult(ResultContext<? extends T> resultContext);
}
