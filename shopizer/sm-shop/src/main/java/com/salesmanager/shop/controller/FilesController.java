package com.salesmanager.shop.controller;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.OutputContentFile;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.utils.FileNameUtils;
import com.salesmanager.shop.utils.FilePathUtils;
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
		FileContentType fileType = FileContentType.STATIC_FILE;
		
		// needs to query the new API
		OutputContentFile file =contentService.getContentFile(storeCode, fileType, new StringBuilder().append(fileName).append(".").append(extension).toString());
		
		
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

		FileContentType fileType = FileContentType.PRODUCT_DIGITAL;
		
		String fileNameAndExtension = new StringBuilder().append(fileName).append(".").append(extension).toString();
		
		// needs to query the new API
		OutputContentFile file = contentService.getContentFile(storeCode, fileType, fileNameAndExtension);
		
		
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
