package com.salesmanager.shop.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Determines if a file name seems to be valid and secure.
 * This utility validates file names to prevent path traversal attacks (CWE-022)
 * 
 * - has an extension
 * - has a name
 * - does not contain path traversal sequences
 * - does not contain special characters
 * @author carlsamson
 *
 */
@Component
public class FileNameUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FileNameUtils.class);
	
	// Pattern to detect path traversal attempts
	private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*[/\\\\]\\.\\.([/\\\\].*)?");
	
	// Pattern to detect null bytes and special characters
	private static final Pattern INVALID_CHARS_PATTERN = Pattern.compile(".*[\\x00<>:\"|?*].*");
	
	// Maximum filename length
	private static final int MAX_FILENAME_LENGTH = 255;
	
	/**
	 * Validates file name for basic requirements
	 * @param fileName the filename to validate
	 * @return true if valid, false otherwise
	 */
	public boolean validFileName(String fileName) {
		
		boolean validName = true;
		
		//has an extention
		if(StringUtils.isEmpty(FilenameUtils.getExtension(fileName))) {
			validName = false;
		}
		
		//has a filename
		if(StringUtils.isEmpty(FilenameUtils.getBaseName(fileName))) {
			validName = false;
		}
		
		return validName;
	}
	
	/**
	 * Validates and sanitizes file name to prevent path traversal attacks (CWE-022)
	 * @param fileName the filename to validate
	 * @return true if the filename is secure, false otherwise
	 */
	public boolean isSecureFileName(String fileName) {
		if (StringUtils.isEmpty(fileName)) {
			LOGGER.warn("File name is null or empty");
			return false;
		}
		
		// Check filename length
		if (fileName.length() > MAX_FILENAME_LENGTH) {
			LOGGER.warn("File name exceeds maximum length: {}", fileName);
			return false;
		}
		
		// Check for path traversal patterns (../, ..\)
		if (PATH_TRAVERSAL_PATTERN.matcher(fileName).matches()) {
			LOGGER.warn("Path traversal attempt detected in filename: {}", fileName);
			return false;
		}
		
		// Check for invalid characters (null bytes, etc.)
		if (INVALID_CHARS_PATTERN.matcher(fileName).matches()) {
			LOGGER.warn("Invalid characters detected in filename: {}", fileName);
			return false;
		}
		
		// Check that filename does not contain directory separators
		if (fileName.contains("/") || fileName.contains("\\")) {
			LOGGER.warn("Directory separator detected in filename: {}", fileName);
			return false;
		}
		
		// Validate basic filename structure
		if (!validFileName(fileName)) {
			LOGGER.warn("File name does not meet basic requirements: {}", fileName);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sanitizes a filename by removing potentially dangerous characters
	 * @param fileName the filename to sanitize
	 * @return sanitized filename or null if invalid
	 */
	public String sanitizeFileName(String fileName) {
		if (StringUtils.isEmpty(fileName)) {
			return null;
		}
		
		// Use Apache Commons to normalize and get just the filename
		String normalized = FilenameUtils.getName(fileName);
		
		// Remove any remaining suspicious characters
		normalized = normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
		
		// Ensure the sanitized name is still valid
		if (!isSecureFileName(normalized)) {
			LOGGER.warn("Failed to sanitize filename: {}", fileName);
			return null;
		}
		
		return normalized;
	}

}
