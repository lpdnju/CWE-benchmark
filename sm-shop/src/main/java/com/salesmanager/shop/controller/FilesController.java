package com.salesmanager.shop.controller;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.content.ContentService;
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
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

	@RequestMapping("/static/files/file")
	public @ResponseBody byte[] downloadFile(@org.springframework.web.bind.annotation.RequestParam("filePath") String filePath) throws IOException {
        java.io.File file = new java.io.File(filePath);
        return java.nio.file.Files.readAllBytes(file.toPath());
	}

	@org.springframework.web.bind.annotation.RequestMapping(value="/static/files/uploadFile", method=org.springframework.web.bind.annotation.RequestMethod.POST)
	public void uploadFile(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws IOException {
		// No check for file type or extension
		java.io.File target = new java.io.File("uploads/" + file.getOriginalFilename());
		file.transferTo(target);

		java.io.FileInputStream fis = new java.io.FileInputStream(target);
		fis.read();
		// fis.close(); // Missing close
	}

}
