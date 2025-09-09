package com.ducnh.ibatis.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;

public class DefaultVFS extends VFS {
	private static final Log log = LogFactory.getLog(DefaultVFS.class);
	
	private static final byte[] JAR_MAGIC = {'P', 'K', 3, 4};
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public List<String> list(URL url, String path) throws IOException {
		InputStream is = null;
		try {
			List<String> resources = new ArrayList<>();
		
			// First, try to find the URL of a JAR file containing the requested resource. If a JAR
			// file is found, then we'll list child resources by reading the JAR.
			
			URL jarUrl = findJarForResources(url);
			if (jarUrl != null) {
				is = jarUrl.openStream();
				if (log.isDebugEnabled()) {
					log.debug("Listing " + url);
				}
				resources = listResources(new JarInputStream(is), path);
			} else {
				List<String> children = new ArrayList<>();
				try {
					if (isJar(url)) {
						is = url.openStream();
						try (JarInputStream jarInput = new JarInputStream(is)) {
							if (log.isDebugEnabled()) {
								log.debug("Listing " + url);
							}
							Path destinationDir = Path.of(path);
							for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null;) {
								if (log.isDebugEnabled()) {
									log.debug("Jar entry: " + entry.getName());
								}
								File entryFile = destinationDir.resolve(entry.getName()).toFile().getCanonicalFile();
								if (!entryFile.getPath().startsWith(destinationDir.toFile().getCanonicalPath())) {
									throw new IOException("Bad zip entry: " + entry.getName());
								}
								children.add(entry.getName());
							}
						}
					} else {
						is = url.openStream();
						List<String> lines = new ArrayList<>();
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
							for (String line; (line = reader.readLine()) != null;) {
								if (log.isDebugEnabled()) {
									log.debug("Reader entry: " + line);
								}
								lines.add(line);
								if (getResources(path + "/" + line).isEmpty()) {
									lines.clear();
									break;
								}
								
							}
						} catch (InvalidPathException | FileSystemException e) {
							lines.clear();
						}
						if (!lines.isEmpty()) {
							if (log.isDebugEnabled()) {
								log.debug("Listing " + url);
							}
							children.addAll(lines);
						}
					}
				} catch (FileNotFoundException e) {
					if (!"file".equals(url.getProtocol())) {
						throw e;
					}
					File file = Path.of(url.getFile()).toFile();
					if (log.isDebugEnabled()) {
						log.debug("Listing directory " + file.getAbsolutePath());
					}
					if (Files.isDirectory(file.toPath())) {
						if (log.isDebugEnabled()) {
							log.debug("Listing " + url);
						}
						children = Arrays.asList(file.list());
					}
				}
				// The URL prefix to use when recursively listing child resources
				String prefix = url.toExternalForm();
				if (!prefix.endsWith("/")) {
					prefix = prefix + "/";
				}
				
				// Iterate over immediate children, adding files and recursing into directories
				for (String child : children) {
					String resourcePath = path + "/" + child;
					resources.add(resourcePath);
					URL childUrl = new URL(prefix + child);
					resources.addAll(list(childUrl, resourcePath));
				}
			} 	
			return resources;	
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					
				}
			}
		}
	}
	
	protected List<String> listResources(JarInputStream jar, String path) throws IOException {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		
		List<String> resources = new ArrayList<>();
		for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
			if (!entry.isDirectory()) {
				StringBuilder name = new StringBuilder(entry.getName());
				if (name.charAt(0) != '/') {
					name.insert(0, '/');
				}
				
				if (name.indexOf(path) == 0) {
					if (log.isDebugEnabled()) {
						log.debug("Found resources: " + name);
					}
					
					resources.add(name.substring(1));
				}
			}
		}
		return resources;
	}
	
	protected URL findJarForResources(URL url) throws MalformedURLException {
		if (log.isDebugEnabled()) {
			log.debug("Find JAR URL: " + url);
		}
		
		boolean continueLoop = true;
		while (continueLoop) {
			try {
				url = new URL(url.getFile());
				if (log.isDebugEnabled()) {
					log.debug("Inner URL: " + url);
				}
			} catch (MalformedURLException e) {
				continueLoop = false;
			}
		} 
		
		// Look for the .jar extension and chop off everything after that
		StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
		int index = jarUrl.lastIndexOf(".jar");
		if (index < 0) {
			if (log.isDebugEnabled()) {
				log.debug("Not a JAR: " + jarUrl);
			}
			return null;
		}
		jarUrl.setLength(index + 4);
		if (log.isDebugEnabled()) {
			log.debug("Extracted JAR URL: " + jarUrl);
		}
		
		// Try to open and test it
		try {
			URL testUrl = new URL(jarUrl.toString());
			if (isJar(testUrl)) {
				return testUrl;
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Not a JAR: " + jarUrl);
			}
			jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
			File file = Path.of(jarUrl.toString()).toFile();
			
			// File name might be URL-encoded
			if (!file.exists()) {
				file = Path.of(URLEncoder.encode(jarUrl.toString(), StandardCharsets.UTF_8)).toFile();
			}
			
			if (file.exists()) {
				if (log.isDebugEnabled()) {
					log.debug("Trying real file: " + file.getAbsolutePath());
				}
				testUrl = file.toURI().toURL();
				if (isJar(testUrl) ) {
					return testUrl;
				}
			}
		} catch (MalformedURLException e) {
			log.warn("Invalid JAR URL: " + jarUrl);
		}
		
		if (log.isDebugEnabled() ) {
			log.debug("Not a Jar: " + jarUrl);
		}
		
		return null;
	}
	
	protected String getPackagePath(String packageName) {
		return packageName == null ? null : packageName.replace('.', '/');
	}
	
	protected boolean isJar(URL url) {
		return isJar(url, new byte[JAR_MAGIC.length]);
	}

	protected boolean isJar(URL url, byte[] buffer) {
		try (InputStream is = url.openStream()) {
			is.read(buffer, 0, JAR_MAGIC.length);
			if (Arrays.equals(buffer, JAR_MAGIC)) {
				if (log.isDebugEnabled()) {
					log.debug("Found JAR: " + url);
				}
				return true;
			}
		} catch (IOException e) {
			
		}
		return false;
	}
}
