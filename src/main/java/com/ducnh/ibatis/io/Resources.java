package com.ducnh.ibatis.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Properties;


public class Resources {
	
	private static final ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();
	
	private static Charset charset;
	private Resources() {
	}
	
	public static ClassLoader getDefaultClassLoader() {
		return classLoaderWrapper.defaultClassLoader;
	}
	
	public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
		classLoaderWrapper.defaultClassLoader = defaultClassLoader;
	}
	
	public static URL getResourceURL(String resource) throws IOException {
		return getResourceURL(null, resource);
	}
	
	
	public static URL getResourceURL(ClassLoader loader, String resource) throws IOException {
		URL url = classLoaderWrapper.getResourceAsURL(resource, loader);
		if (url == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return url;
	}
	
	public static InputStream getResourceAsStream(String resource) throws IOException {
		return getResourceAsStream(null, resource);
	}
	
	public static InputStream getResourceAsStream(ClassLoader loader, String resource) throws IOException {
		InputStream in = classLoaderWrapper.getResourceAsStream(resource, loader);
		if (in == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return in;
	}
	
	public static Properties getResourceAsProperties(String resource) throws IOException {
		Properties props = new Properties();
		try (InputStream in = getResourceAsStream(resource)) {
			props.load(in);
		}
		return props;
	}
	
	public static Properties getResourceAsProperties(ClassLoader classLoader, String resource) throws IOException {
		Properties props = new Properties();
		try (InputStream in = getResourceAsStream(classLoader, resource)) {
			props.load(in);
		}
		return props;
	}
	
	public static Reader getResourceAsReader(String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(resource), charset);
		}
		return reader;
	}
	
	public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(loader, resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
		}
		return reader;
	}
	
	public static File getResourceAsFile(String resource) throws IOException, URISyntaxException {
		return Path.of(getResourceURL(resource).toURI()).toFile();
	}
	
	public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException, URISyntaxException {
		return Path.of(getResourceURL(resource).toURI()).toFile();
	}
	
	public static InputStream getUrlAsStream(String urlString) throws IOException {
		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();
		return conn.getInputStream();
	}
	
	public static Reader getUrlAsReader(String urlString) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getUrlAsStream(urlString));
		} else {
			reader = new InputStreamReader(getUrlAsStream(urlString), charset);
		}
		return reader;
	}
	
	public static Properties getUrlAsProperties(String urlString) throws IOException {
		Properties props = new Properties();
		try (InputStream in = getUrlAsStream(urlString)) {
			props.load(in);
		}
		return props;
	} 
	
	public static Class<?> classForName(String className) throws ClassNotFoundException {
		return classLoaderWrapper.classForName(className);
	}
	
	public static Charset getCharset() {
		return charset;
	}
	
	public static void setCharset(Charset charset) {
		Resources.charset = charset;
	}
}
