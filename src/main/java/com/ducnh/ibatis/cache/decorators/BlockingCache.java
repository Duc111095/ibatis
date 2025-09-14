package com.ducnh.ibatis.cache.decorators;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.ducnh.ibatis.cache.Cache;
import com.ducnh.ibatis.cache.CacheException;

public class BlockingCache implements Cache {
	
	private long timeout;
	private final Cache delegate;
	private final ConcurrentHashMap<Object, CountDownLatch> locks;
	
	public BlockingCache(Cache delegate) {
		this.delegate = delegate;
		this.locks = new ConcurrentHashMap<>();
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
		try {
			delegate.putObject(key, value);
		} finally {
			releaseLock(key);
		}
	}
	
	@Override
	public Object getObject(Object key) {
		acquireLock(key);
		Object value = delegate.getObject(key);
		if (value != null) {
			releaseLock(key);
		}
		return value;
	}
	
	@Override
	public Object removeObject(Object key) {
		releaseLock(key);
		return null;
	}
	
	@Override
	public void clear() {
		delegate.clear();
	}
	
	private void acquireLock(Object key) {
		CountDownLatch newLatch = new CountDownLatch(1);
		while (true) {
			CountDownLatch latch = locks.putIfAbsent(key, newLatch);
			if (latch == null) {
				break;
			}
			try {
				if (timeout > 0) {
					boolean acquired = latch.await(timeout, TimeUnit.MILLISECONDS);
					if (!acquired) {
						throw new CacheException(
							"Couldn't get a lock in " + timeout + " for the key " + key + " at the cache " + delegate.getId());
					}
				} else {
					latch.await();
				}
			} catch (InterruptedException e) {
				throw new CacheException("Got interuppted while trying to acquire lock for key " + key, e);
			}
		}
	}
	
	private void releaseLock(Object key) {
		CountDownLatch latch = locks.remove(key);
		if (latch == null) {
			throw new IllegalStateException("Detected an attempt at releasing unacquired lock. This should never happen.");
		}
		latch.countDown();
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
