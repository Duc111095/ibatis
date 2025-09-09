package com.ducnh.ibatis.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.ducnh.ibatis.logging.Log;
import com.ducnh.ibatis.logging.LogFactory;

public class ExternalResources {
	
	private static final Log log = LogFactory.getLog(ExternalResources.class);
	 
	private ExternalResources() {
		
	}
	
	public static void copyExternalResource(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}
		
		try (InputStream source = Files.newInputStream(sourceFile.toPath());
			OutputStream destination = Files.newOutputStream(destFile.toPath())) {
			source.transferTo(destination);
		}
	} 
	
	public static String getConfiguredTemplate(String templatePath, String templateProperty) throws FileNotFoundException{
		String templateName = "";
		Properties migrationProperties = new Properties();
		
		try (InputStream is = Files.newInputStream(Path.of(templatePath))) {
			migrationProperties.load(is);
			templateName = migrationProperties.getProperty(templateProperty);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (Exception e) {
			log.error("", e);
		}
		
		return templateName;
	}
}
