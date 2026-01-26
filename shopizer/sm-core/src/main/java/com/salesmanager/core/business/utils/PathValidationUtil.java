package com.salesmanager.core.business.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for secure path validation to prevent path traversal attacks (CWE-022).
 * 
 * This utility provides methods to validate file paths and ensure they remain within
 * designated base directories, preventing directory traversal attacks using sequences
 * like "../" or absolute paths.
 * 
 * @author Shopizer Security Team
 * @since 2026-01-26
 */
public class PathValidationUtil {
    
    /**
     * Validates that a file path is safe and within the specified base directory.
     * 
     * This method performs the following security checks:
     * 1. Normalizes the base directory path
     * 2. Resolves the input path against the base directory
     * 3. Normalizes the resulting path to remove any ".." or "." components
     * 4. Verifies the final path starts with the base directory
     * 
     * @param baseDirectory the base directory path (must be absolute)
     * @param inputPath the user-provided file path to validate
     * @return the validated, normalized absolute Path
     * @throws SecurityException if the path is invalid or attempts directory traversal
     * @throws IllegalArgumentException if base directory is null or not absolute
     */
    public static Path validatePath(String baseDirectory, String inputPath) throws SecurityException {
        // Input validation
        if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("Base directory cannot be null or empty");
        }
        
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Input path cannot be null or empty");
        }
        
        try {
            // Normalize base directory path
            Path basePath = Paths.get(baseDirectory).toAbsolutePath().normalize();
            
            // Check if base directory is absolute
            if (!basePath.isAbsolute()) {
                throw new IllegalArgumentException("Base directory must be an absolute path");
            }
            
            // Resolve the input path against the base directory and normalize
            Path resolvedPath = basePath.resolve(inputPath).normalize();
            
            // Security check: verify the resolved path starts with the base path
            // This prevents path traversal attacks using "../" sequences
            if (!resolvedPath.startsWith(basePath)) {
                throw new SecurityException(
                    "Path traversal attempt detected: Path '" + inputPath + 
                    "' attempts to escape base directory '" + baseDirectory + "'"
                );
            }
            
            return resolvedPath;
            
        } catch (java.nio.file.InvalidPathException e) {
            throw new SecurityException("Invalid path format: " + inputPath, e);
        }
    }
    
    /**
     * Validates a file path and returns the File object if safe.
     * 
     * @param baseDirectory the base directory path (must be absolute)
     * @param inputPath the user-provided file path to validate
     * @return the validated File object
     * @throws SecurityException if the path is invalid or attempts directory traversal
     * @throws IllegalArgumentException if base directory is null or not absolute
     */
    public static File validateFile(String baseDirectory, String inputPath) throws SecurityException {
        Path validatedPath = validatePath(baseDirectory, inputPath);
        return validatedPath.toFile();
    }
    
    /**
     * Validates a file path and checks if the file exists.
     * 
     * @param baseDirectory the base directory path (must be absolute)
     * @param inputPath the user-provided file path to validate
     * @return true if the path is valid and the file exists, false otherwise
     * @throws SecurityException if the path is invalid or attempts directory traversal
     * @throws IllegalArgumentException if base directory is null or not absolute
     */
    public static boolean isValidAndExists(String baseDirectory, String inputPath) throws SecurityException {
        Path validatedPath = validatePath(baseDirectory, inputPath);
        return validatedPath.toFile().exists();
    }
    
    /**
     * Sanitizes a filename by removing or replacing potentially dangerous characters.
     * 
     * This method removes path separators and parent directory references to prevent
     * path traversal attacks when the filename is used in file operations.
     * 
     * @param filename the filename to sanitize
     * @return the sanitized filename
     * @throws IllegalArgumentException if filename is null or empty after sanitization
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        // Remove path separators and parent directory references
        String sanitized = filename.replaceAll("[/\\\\]", "")
                                   .replaceAll("\\.\\.", "")
                                   .trim();
        
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Filename becomes empty after sanitization");
        }
        
        return sanitized;
    }
    
    /**
     * Alias for sanitizeFilename() to maintain naming consistency.
     * 
     * @param fileName the file name to sanitize
     * @return the sanitized file name
     * @throws IllegalArgumentException if fileName is null or empty after sanitization
     */
    public static String sanitizeFileName(String fileName) {
        return sanitizeFilename(fileName);
    }
}
