package com.ducnh.ibatis.executor.loader.javassist;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.ducnh.ibatis.executor.ExecutorException;
import com.ducnh.ibatis.executor.loader.AbstractEnhancedDeserializationProxy;
import com.ducnh.ibatis.executor.loader.AbstractSerialStateHolder;
import com.ducnh.ibatis.executor.loader.ResultLoaderMap;
import com.ducnh.ibatis.executor.loader.WriteReplaceInterface;
import com.ducnh.ibatis.io.Resources;
import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;
import com.ducnh.ibatis.reflection.ExceptionUtil;
import com.ducnh.ibatis.reflection.factory.ObjectFactory;
import com.ducnh.ibatis.reflection.property.PropertyCopier;
import com.ducnh.ibatis.reflection.property.PropertyNamer;
import com.ducnh.ibatis.session.Configuration;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.Proxy;

public class JavassistProxyFactory implements com.ducnh.ibatis.executor.loader.ProxyFactory{

	private static final String FINALIZE_METHOD = "finalize";
	private static final String WRITE_REPLACE_METHOD = "writeReplace";
	
	public JavassistProxyFactory() {
		try {
			Resources.classForName("javassist.util.proxy.ProxyFactory");
		} catch (Throwable e) {
			throw new IllegalStateException(
				"Cannot enable lazy loading because Javassist is not available. Add javassist to your classpath.", e);
		}
	}
	
	@Override
	public Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
		ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		return EnhancedResultObjectProxyImpl.createProxy(target, lazyLoader, configuration, objectFactory, 
			constructorArgTypes, constructorArgs);
	}

	public Object createDeserializationProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
		ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		return EnhancedDeserializationProxyImpl.createProxy(target, unloadedProperties, objectFactory, constructorArgTypes, 
			constructorArgs);
	} 
	
	static Object createStaticProxy(Class<?> type, MethodHandler callback, List<Class<?>> constructorArgTypes,
		List<Object> constructorArgs) {
		ProxyFactory enhancer = new ProxyFactory();
		enhancer.setSuperclass(type);
	
		try {
			type.getDeclaredMethod(WRITE_REPLACE_METHOD);
			if (LogHolder.log.isDebugEnabled()) {
				LogHolder.log.debug(WRITE_REPLACE_METHOD + " method was found on bean " + type + ", make sure it returns this");
			}
		} catch (NoSuchMethodException e) {
			enhancer.setInterfaces(new Class[] {WriteReplaceInterface.class});
		} catch (SecurityException e) {
			
		}
		
		Object enhanced;
		Class<?>[] typesArray = constructorArgTypes.toArray(new Class[constructorArgTypes.size()]);
		Object[] valuesArray = constructorArgs.toArray(new Object[constructorArgs.size()]);
		try {
			enhanced = enhancer.create(typesArray, valuesArray);
		} catch (Exception e) {
			throw new ExecutorException("Error creating lazy proxy. Cause: " + e, e);
		}
		
		((Proxy) enhanced).setHandler(callback);
		return enhanced;
	}
	
	private static class EnhancedResultObjectProxyImpl implements MethodHandler {
		private final Class<?> type;
		private final ResultLoaderMap lazyLoader;
		private final boolean aggressive;
		private final Set<String> lazyLoadTriggerMethods;
		private final ObjectFactory objectFactory;
		private final List<Class<?>> constructorArgTypes;
		private final List<Object> constructorArgs;
		private final ReentrantLock lock = new ReentrantLock();
		
		private EnhancedResultObjectProxyImpl(Class<?> type, ResultLoaderMap lazyLoader, Configuration configuration,
			ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
			this.type = type;
			this.lazyLoader = lazyLoader;
			this.aggressive = configuration.isAggressiveLazyLoading();
			this.lazyLoadTriggerMethods = configuration.getLazyLoadTriggerMethods();
			this.objectFactory = objectFactory;
			this.constructorArgTypes = constructorArgTypes;
			this.constructorArgs = constructorArgs;
		}
		
		public static Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
			ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
			final Class<?> type = target.getClass();
			EnhancedResultObjectProxyImpl callback = new EnhancedResultObjectProxyImpl(type, lazyLoader, configuration, 
				objectFactory, constructorArgTypes, constructorArgs);
			Object enhanced = createStaticProxy(type, callback, constructorArgTypes, constructorArgs);
			PropertyCopier.copyBeanProperties(type, target, enhanced);
			return enhanced;
		}
	
		@Override
		public Object invoke(Object enhanced, Method method, Method methodProxy, Object[] args) throws Throwable {
			final String methodName = method.getName();
			lock.lock();
			try {
				if (WRITE_REPLACE_METHOD.equals(methodName)) {
					Object original;
					if (constructorArgTypes.isEmpty()) {
						original = objectFactory.create(type);
					} else {
						original = objectFactory.create(type, constructorArgTypes, constructorArgs);
					}
					PropertyCopier.copyBeanProperties(type, enhanced, original);
					if (!lazyLoader.isEmpty()) {
						return new JavassistSerialStateHolder(original, lazyLoader.getProperties(), objectFactory, 
							constructorArgTypes, constructorArgs);
					} else {
						return original;
					}
				}
				if (!lazyLoader.isEmpty() && !FINALIZE_METHOD.equals(methodName)) {
					if (aggressive || lazyLoadTriggerMethods.contains(methodName)) {
						lazyLoader.loadAll();
					} else if (PropertyNamer.isSetter(methodName)) {
						final String property = PropertyNamer.methodToProperty(methodName);
						lazyLoader.remove(property);
					} else if (PropertyNamer.isGetter(methodName)) {
						final String property = PropertyNamer.methodToProperty(methodName);
						if (lazyLoader.hasLoader(property)) {
							lazyLoader.load(property);
						}
					}
				}
				return methodProxy.invoke(enhanced, args);
			} catch (Throwable t) {
				throw ExceptionUtil.unwrapThrowable(t);
			} finally {
				lock.unlock();
			}
		}
	}
	
	private static class EnhancedDeserializationProxyImpl extends AbstractEnhancedDeserializationProxy
		implements MethodHandler {
		
		private EnhancedDeserializationProxyImpl(Class<?> type, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
			ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
			super(type, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
		}
		
		public static Object createProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
			ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
			final Class<?> type = target.getClass();
			EnhancedDeserializationProxyImpl callback = new EnhancedDeserializationProxyImpl(type, unloadedProperties,
				objectFactory, constructorArgTypes, constructorArgs);
			Object enhanced = createStaticProxy(type, callback, constructorArgTypes, constructorArgs);
			PropertyCopier.copyBeanProperties(type, target, enhanced);
			return enhanced;
		}
		
		@Override
		public Object invoke(Object enhanced, Method method, Method methodProxy, Object[] args) throws Throwable {
			final Object o = super.invoke(enhanced, methodProxy, args);
			return o instanceof AbstractSerialStateHolder ? o : methodProxy.invoke(o, args);
		}
		
		@Override
		public AbstractSerialStateHolder newSerialStateHolder(Object userBean,
			Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
			List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
			return new JavassistSerialStateHolder(userBean, unloadedProperties, objectFactory, constructorArgTypes,
				constructorArgs);
		}
	}
	
	private static class LogHolder {
		private static final Log log = LogFactory.getLog(JavassistProxyFactory.class);
	}
}
