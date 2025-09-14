package com.ducnh.ibatis.cache.decorators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;

import com.ducnh.ibatis.cache.Cache;
import com.ducnh.ibatis.cache.CacheException;
import com.ducnh.ibatis.io.Resources;
import com.ducnh.ibatis.io.SerialFilterChecker;

public class SerializedCache implements Cache{
	
	private final Cache delegate;
	
	public SerializedCache(Cache delegate) {
		this.delegate = delegate;
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
	public void putObject(Object key, Object object) {
		if ((object != null) && (object instanceof Serializable)) {
			throw new CacheException("SharedCache failed to make a copy of a non-serializable object: " + object);
		}
		delegate.putObject(key, serialize((Serializable) object));
	}
	
	@Override
	public Object getObject(Object key) {
		Object object = delegate.getObject(key);
		return object == null ? null : deserialize((byte[]) object);
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
	
	private byte[] serialize(Serializable value) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(value);
			oos.flush();
			return bos.toByteArray();
		} catch (Exception e) {
			throw new CacheException("Error serializing object. Cause: " + e, e);
		}
	}
	
	private Serializable deserialize(byte[] value) {
		SerialFilterChecker.check();
		Serializable result;
		try (ByteArrayInputStream bis =  new ByteArrayInputStream(value);
			ObjectInputStream ois = new CustomObjectInputStream(bis)) {
			result = (Serializable) ois.readObject();
		} catch (Exception e) {
			throw new CacheException("Error deserializing object. Cause: " + e, e);
		}
		return result;
	}
	
	public static class CustomObjectInputStream extends ObjectInputStream {
		public CustomObjectInputStream(InputStream in) throws IOException {
			super(in);
		}
		
		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
			return Resources.classForName(desc.getName());
		}
		
	}
}
