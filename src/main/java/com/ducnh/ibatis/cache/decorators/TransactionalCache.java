package com.ducnh.ibatis.cache.decorators;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ducnh.ibatis.cache.Cache;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;

public class TransactionalCache implements Cache{
	
	private static final Log log = LogFactory.getLog(TransactionalCache.class);
	
	private final Cache delegate;
	private boolean clearOnCommit;
	private final Map<Object, Object> entriesToAddOnCommit;
	private final Set<Object> entriesMissedInCache;
	
	public TransactionalCache(Cache delegate) {
		this.delegate = delegate;
		this.clearOnCommit = false;
		this.entriesMissedInCache = new HashSet<>();
		this.entriesToAddOnCommit = new HashMap<>();
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
	public Object getObject(Object key) {
		Object object = delegate.getObject(key);
		if (object == null) {
			entriesMissedInCache.add(key);
		}
		if (clearOnCommit) {
			return null;
		}
		return object;
	}
	
	@Override
	public void putObject(Object key, Object object) {
		entriesToAddOnCommit.put(key, object);
	}
	
	@Override
	public Object removeObject(Object key) {
		return null;
	}
	
	@Override
	public void clear() {
		clearOnCommit = true;
		entriesToAddOnCommit.clear();
	}
	
	public void commit() {
		if (clearOnCommit) {
			delegate.clear();
		}
		flushPendingEntries();
		reset();
	}
	
	public void rollback() {
		unlockMissedEntries();
		reset();
	}
	
	private void reset() {
		clearOnCommit = false;
		entriesToAddOnCommit.clear();
		entriesMissedInCache.clear();
	}
	
	private void flushPendingEntries() {
		for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
			delegate.putObject(entry.getKey(), entry.getValue());
		}
		for (Object entry : entriesMissedInCache) {
			if (!entriesToAddOnCommit.containsKey(entry)) {
				delegate.putObject(entry, null);
			}
		}
	}
	
	private void unlockMissedEntries() {
		for (Object entry : entriesMissedInCache) {
			try {
				delegate.removeObject(entry);
			} catch (Exception e) {
				log.warn("Unexpected exception while notifying a rollback to the cache adapter. "
					+ "Consider upgrading your cache adapter to the latest version. Cause: " + e);
			}
		}
	}
}
