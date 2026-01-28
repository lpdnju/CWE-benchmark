package com.salesmanager.shop.controller;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.OutputContentFile;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.utils.FileNameUtils;
import com.salesmanager.shop.utils.FilePathUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class FilesController extends AbstractController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FilesController.class);
	
	private static final String BASE_DIRECTORY = "/var/files/";
	
	@Inject
	private ContentService contentService;
	
	@Inject
	private FileNameUtils fileNameUtils;
	
	@Inject
	private FilePathUtils filePathUtils;
	

	/**
	 * Validates and sanitizes a path parameter to prevent path traversal attacks (CWE-022)
	 * @param param the parameter to sanitize
	 * @return sanitized parameter
	 * @throws ServiceException if parameter contains invalid characters
	 */
	private String sanitizePathParameter(String param) throws ServiceException {
		if (StringUtils.isEmpty(param)) {
			throw new ServiceException("Parameter cannot be null or empty");
		}
		
		// Use FilenameUtils to extract just the name without path components
		String sanitized = FilenameUtils.getName(param);
		
		// Check for path traversal patterns
		if (param.contains("..") || param.contains("/") || param.contains("\\") || param.contains("\0")) {
			LOGGER.error("Path traversal attempt detected in parameter: {}", param);
			throw new ServiceException("Invalid parameter: contains path traversal characters");
		}
		
		// Validate that we still have a valid name after checks
		if (StringUtils.isEmpty(sanitized) || !sanitized.equals(param)) {
			LOGGER.error("Parameter validation failed: {}", param);
			throw new ServiceException("Invalid parameter format");
		}
		
		return sanitized;
	}
	

	/**
	 * Serves static files (css, js ...) the repository is a single node by merchant
	 * @param storeCode
	 * @param extension
	 * @return
	 * @throws IOException
	 * @throws ServiceException
	 */
	@RequestMapping("/static/files/{storeCode}/{fileName}.{extension}")
	public @ResponseBody byte[] downloadFile(@PathVariable final String storeCode, @PathVariable final String fileName, @PathVariable final String extension, HttpServletRequest request, HttpServletResponse response) throws IOException, ServiceException {

		// example -> /files/<store code>/myfile.css
		
		// Validate and sanitize all path parameters to prevent path traversal (CWE-022)
		String sanitizedStoreCode = sanitizePathParameter(storeCode);
		String sanitizedFileName = sanitizePathParameter(fileName);
		String sanitizedExtension = sanitizePathParameter(extension);
		
		FileContentType fileType = FileContentType.STATIC_FILE;
		
		// needs to query the new API
		OutputContentFile file =contentService.getContentFile(sanitizedStoreCode, fileType, new StringBuilder().append(sanitizedFileName).append(".").append(sanitizedExtension).toString());
		
		
		if(file!=null) {
			return file.getFile().toByteArray();
		} else {
			LOGGER.debug("File not found " + fileName + "." + extension);
			response.sendError(404, Constants.FILE_NOT_FOUND);
			return null;
		}
	}
	
	/**
	 * Requires admin with roles admin, superadmin or product
	 * @param storeCode
	 * @param fileName
	 * @param extension
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping("/admin/files/downloads/{storeCode}/{fileName}.{extension}")
	public @ResponseBody byte[] downloadProduct(@PathVariable final String storeCode, @PathVariable final String fileName, @PathVariable final String extension, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Validate and sanitize all path parameters to prevent path traversal (CWE-022)
		String sanitizedStoreCode = sanitizePathParameter(storeCode);
		String sanitizedFileName = sanitizePathParameter(fileName);
		String sanitizedExtension = sanitizePathParameter(extension);
		
		FileContentType fileType = FileContentType.PRODUCT_DIGITAL;
		
		String fileNameAndExtension = new StringBuilder().append(sanitizedFileName).append(".").append(sanitizedExtension).toString();
		
		// needs to query the new API
		OutputContentFile file = contentService.getContentFile(sanitizedStoreCode, fileType, fileNameAndExtension);
		
		
		if(file!=null) {
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileNameAndExtension + "\"");
			return file.getFile().toByteArray();
		} else {
			LOGGER.debug("File not found " + fileName + "." + extension);
			response.sendError(404, Constants.FILE_NOT_FOUND);
			return null;
		}
	}

	/**
	 * Downloads a file by path with proper security validation to prevent path traversal (CWE-022)
	 * @param filePath user-provided file path
	 * @param response HTTP response
	 * @return file contents as byte array
	 * @throws IOException if file operations fail
	 */
	@RequestMapping("/files/download")
	public @ResponseBody byte[] downloadFileByPath(@RequestParam("filePath") String filePath, HttpServletResponse response) throws IOException {
		// Validate input is not empty
		if (filePath == null || filePath.trim().isEmpty()) {
			LOGGER.warn("File path parameter is null or empty");
			response.sendError(400, "File path is required");
			return null;
		}
		
		// Extract filename from path and validate it
		String fileName = Paths.get(filePath).getFileName().toString();
		if (!fileNameUtils.isSecureFileName(fileName)) {
			LOGGER.warn("Invalid or insecure filename detected: {}", fileName);
			response.sendError(400, "Invalid file name");
			return null;
		}
		
		// Securely resolve the path within the base directory
		Path securedPath = filePathUtils.secureResolvePath(BASE_DIRECTORY, filePath);
		if (securedPath == null) {
			LOGGER.warn("Path traversal attempt detected for filePath: {}", filePath);
			response.sendError(403, "Access denied");
			return null;
		}
		
		// Verify the file exists and is a regular file (not a directory)
		File file = securedPath.toFile();
		if (!file.exists()) {
			LOGGER.debug("File not found: {}", securedPath);
			response.sendError(404, Constants.FILE_NOT_FOUND);
			return null;
		}
		
		if (!file.isFile()) {
			LOGGER.warn("Attempted to access non-file resource: {}", securedPath);
			response.sendError(400, "Invalid file");
			return null;
		}
		
		try {
			// Set secure headers
			response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
			response.setHeader("X-Content-Type-Options", "nosniff");
			
			// Read and return file contents
			byte[] fileContent = Files.readAllBytes(securedPath);
			LOGGER.info("File downloaded successfully: {}", securedPath.getFileName());
			return fileContent;
		} catch (IOException e) {
			LOGGER.error("Error reading file: {}", securedPath, e);
			response.sendError(500, "Error reading file");
			return null;
		}
	}

}
