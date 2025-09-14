package com.ducnh.ibatis.cache.decorators;

import java.util.concurrent.locks.ReentrantLock;

import com.ducnh.ibatis.cache.Cache;

public class SynchronizedCache implements Cache{

	private final ReentrantLock lock = new ReentrantLock();
	private final Cache delegate;
	
	public SynchronizedCache(Cache delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}
	
	@Override
	public int getSize() {
		lock.lock();
		try {
			return delegate.getSize();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void putObject(Object key, Object object) {
		lock.lock();
		try {
			delegate.putObject(key, object);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public Object getObject(Object key) {
		lock.lock();
		try {
			return delegate.getObject(key);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public Object removeObject(Object key) {
		lock.lock();
		try {
			return delegate.removeObject(key);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void clear() {
		lock.lock();
		try {
			delegate.clear();
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}
}
