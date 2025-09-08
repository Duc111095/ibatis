package com.ducnh.ibatis.io;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;

public class ResolverUtil<T> {

	private static final Log log = LogFactory.getLog(ResolverUtil.class);
	public interface Test {
		boolean matches(Class<?> Type);
	}
	
	public static class IsA implements Test {
		private final Class<?> parent;
		public IsA(Class<?> parentType) {
			this.parent = parentType;
		}
		
		@Override
		public boolean matches(Class<?> type) {
			return type != null && parent.isAssignableFrom(type);
		}
		
		@Override
		public String toString() {
			return "is assignable to " + parent.getSimpleName();
		}
	}
	
	public static class AnnotatedWith implements Test {
		private final Class<? extends Annotation> annotation;
		public AnnotatedWith(Class<? extends Annotation> annotation) {
			this.annotation = annotation;
		}
		
		@Override
		public boolean matches(Class<?> type) {
			return type != null && type.isAnnotationPresent(annotation);
		}
		
		@Override
		public String toString() {
			return "annotated with @" + annotation.getSimpleName();
		}
	}
	
	private Set<Class<? extends T>> matches = new HashSet<>();
	private ClassLoader classloader;
	public Set<Class<? extends T>> getClasses() {
		return matches;
	}
	
	public ClassLoader getClassLoader() {
		return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
	}
	
	public void setClassLoader(ClassLoader classloader) {
		this.classloader = classloader;
	}
	
	public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		
		Test test = new IsA(parent);
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		return this;
	}
	
	public ResolveUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
		if (packageNames == null) {
			return this;
		}
		Test test =  new AnnotatedWith(annotation);
		for (String pkg : packageNames) {
			find(test, pkg);
		}
		
		return this;
	}
}
