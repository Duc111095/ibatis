package com.ducnh.ibatis.cache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import com.ducnh.ibatis.cache.Cache;

public class SoftCache implements Cache{
	private final Deque<Object> hardLinksToAvoidGarbageCollection;
	private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
	private final Cache delegate;
	private int numberOfHardLinks;
	private final ReentrantLock lock = new ReentrantLock();
	
	public SoftCache(Cache delegate) {
		this.delegate = delegate;
		this.numberOfHardLinks = 256;
		this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
		this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}
	
	@Override
	public int getSize() {
		removeGarbageCollectedItems();
		return delegate.getSize();
	}
	
	@Override
	public void putObject(Object key, Object value) {
		removeGarbageCollectedItems();
		delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
	}
	
	@Override
	public Object getObject(Object key) {
		Object result = null;
		@SuppressWarnings("unchecked")
		SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
		if (softReference != null) {
			result = softReference.get();
			if (result == null) {
				delegate.removeObject(key);
			} else {
				lock.lock();
				try {
					hardLinksToAvoidGarbageCollection.addFirst(result);
					if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
						hardLinksToAvoidGarbageCollection.removeLast();
					}
				} finally {
					lock.unlock();
				}
			}
		}
		return result;
	}
	
	@Override
	public Object removeObject(Object key) {
		removeGarbageCollectedItems();
		@SuppressWarnings("unchecked")
		SoftReference<Object> softReference = (SoftReference<Object>) delegate.removeObject(key);
		return softReference == null ? null : softReference.get();
	}
	
	@Override
	public void clear() {
		lock.lock();
		try {
			hardLinksToAvoidGarbageCollection.clear();
		} finally {
			lock.unlock();
		}
		removeGarbageCollectedItems();
		delegate.clear();
	}
	
	private void removeGarbageCollectedItems() {
		SoftEntry sv;
		while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
			delegate.removeObject(sv.key);
		}
	}
	
	private static class SoftEntry extends SoftReference<Object> {
		private final Object key;
		
		SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
			super(value, garbageCollectionQueue);
			this.key = key;
		}
	}
}
