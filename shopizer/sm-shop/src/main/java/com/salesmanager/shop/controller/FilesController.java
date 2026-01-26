package com.salesmanager.shop.controller;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.business.utils.PathValidationUtil;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.OutputContentFile;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.store.controller.AbstractController;
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
	

	
	@Inject
	private ContentService contentService;
	

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
	 * Downloads a file by path with path traversal protection.
	 * 
	 * SECURITY: This method previously had a critical CWE-022 vulnerability where user input
	 * was directly concatenated to form file paths, allowing attackers to access files outside
	 * the intended directory using path traversal sequences like "../".
	 * 
	 * FIX: Now uses PathValidationUtil.validatePath() to ensure the requested file path
	 * remains within the designated base directory.
	 * 
	 * @param filePath the relative path to the file (user input - untrusted)
	 * @param response HTTP response
	 * @return file contents as byte array
	 * @throws IOException if file operations fail
	 */
	@RequestMapping("/files/download")
	public @ResponseBody byte[] downloadFileByPath(@RequestParam("filePath") String filePath, HttpServletResponse response) throws IOException {
		String baseDir = "/var/files/";
		
		try {
			// SECURITY FIX: Validate path to prevent directory traversal attacks (CWE-022)
			Path validatedPath = PathValidationUtil.validatePath(baseDir, filePath);
			File file = validatedPath.toFile();
			
			if (file.exists()) {
				response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
				return Files.readAllBytes(validatedPath);
			} else {
				LOGGER.debug("File not found: " + filePath);
				response.sendError(404, Constants.FILE_NOT_FOUND);
				return null;
			}
		} catch (SecurityException e) {
			// Path traversal attempt detected
			LOGGER.warn("Security violation - path traversal attempt detected: " + filePath, e);
			response.sendError(403, "Access denied: Invalid file path");
			return null;
		}
	}

}
