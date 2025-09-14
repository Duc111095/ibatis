package com.ducnh.ibatis.cache.decorators;

import com.ducnh.ibatis.cache.Cache;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;

public class LoggingCache implements Cache{

	private final Log log;
	private final Cache delegate;
	protected int requests;
	protected int hits;
	
	public LoggingCache(Cache delegate) {
		this.delegate = delegate;
		this.log = LogFactory.getLog(getId());
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}
	
	@Override
	public int getSize() {
		return delegate.getSize();
	}
	
	@Override
	public void putObject(Object key, Object value) {
		delegate.putObject(key, value);
	}
	
	@Override
	public Object getObject(Object key) {
		requests++;
		final Object value = delegate.getObject(key);
		if (value != null) {
			hits++;
		}
		if (log.isDebugEnabled()) {
			log.debug("Cache Hit Radio [" + getId() + "]: " + getHitRatio());
		}
		return value;
	}
	
	@Override
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}
	
	@Override
	public void clear() {
		delegate.clear();
	}
	
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
	
	private double getHitRatio() {
		return (double) hits / (double) requests;
	}
}
