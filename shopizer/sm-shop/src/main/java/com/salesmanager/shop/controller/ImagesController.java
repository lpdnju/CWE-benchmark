package com.salesmanager.shop.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.image.ProductImageService;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.catalog.product.file.ProductImageSize;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.OutputContentFile;
import org.apache.commons.io.FilenameUtils;

/**
 * When handling images and files from the application server
 * @author c.samson
 *
 */
@Controller
public class ImagesController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ImagesController.class);
	

	
	@Inject
	private ContentService contentService;
	
	@Inject
	private ProductImageService productImageService;
	
	private byte[] tempImage = null;
	
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
	
	@PostConstruct
	public void init() {
		try {
			File file = ResourceUtils.getFile("classpath:static/not-found.png");
			if(file != null) {
				byte[] bFile = Files.readAllBytes(file.toPath());
				this.tempImage = bFile;
			}

			
		} catch (Exception e) {
			LOGGER.error("Can't load temporary default image", e);
		}
	}
	
	/**
	 * Logo, content image
	 * @param storeId
	 * @param imageType (LOGO, CONTENT, IMAGE)
	 * @param imageName
	 * @return
	 * @throws IOException
	 * @throws ServiceException 
	 */
	@RequestMapping("/static/files/{storeCode}/{imageType}/{imageName}.{extension}")
	public @ResponseBody byte[] printImage(@PathVariable final String storeCode, @PathVariable final String imageType, @PathVariable final String imageName, @PathVariable final String extension) throws IOException, ServiceException {

		// example -> /static/files/DEFAULT/CONTENT/myImage.png
		
		// Validate and sanitize all path parameters to prevent path traversal (CWE-022)
		String sanitizedStoreCode = sanitizePathParameter(storeCode);
		String sanitizedImageType = sanitizePathParameter(imageType);
		String sanitizedImageName = sanitizePathParameter(imageName);
		String sanitizedExtension = sanitizePathParameter(extension);
		
		FileContentType imgType = null;
		
		if(FileContentType.LOGO.name().equals(sanitizedImageType)) {
			imgType = FileContentType.LOGO;
		}
		
		if(FileContentType.IMAGE.name().equals(sanitizedImageType)) {
			imgType = FileContentType.IMAGE;
		}
		
		if(FileContentType.PROPERTY.name().equals(sanitizedImageType)) {
			imgType = FileContentType.PROPERTY;
		}
		
		OutputContentFile image =contentService.getContentFile(sanitizedStoreCode, imgType, new StringBuilder().append(sanitizedImageName).append(".").append(sanitizedExtension).toString());
		
		
		if(image!=null) {
			return image.getFile().toByteArray();
		} else {
			return tempImage;
		}

	}
	

	/**
	 * For product images
	 * @Deprecated
	 * @param storeCode
	 * @param productCode
	 * @param imageType
	 * @param imageName
	 * @param extension
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("/static/{storeCode}/{imageType}/{productCode}/{imageName}.{extension}")
	public @ResponseBody byte[] printImage(@PathVariable final String storeCode, @PathVariable final String productCode, @PathVariable final String imageType, @PathVariable final String imageName, @PathVariable final String extension) throws IOException {

		// product image
		// example small product image -> /static/DEFAULT/products/TB12345/product1.jpg
		
		// example large product image -> /static/DEFAULT/products/TB12345/product1.jpg

		
		/**
		 * List of possible imageType
		 * 
		 */
		
		// Validate and sanitize all path parameters to prevent path traversal (CWE-022)
		OutputContentFile image = null;
		try {
			String sanitizedStoreCode = sanitizePathParameter(storeCode);
			String sanitizedProductCode = sanitizePathParameter(productCode);
			String sanitizedImageType = sanitizePathParameter(imageType);
			String sanitizedImageName = sanitizePathParameter(imageName);
			String sanitizedExtension = sanitizePathParameter(extension);

			ProductImageSize size = ProductImageSize.SMALL;
			
			if(sanitizedImageType.equals(FileContentType.PRODUCTLG.name())) {
				size = ProductImageSize.LARGE;
			} 
			

			
			image = productImageService.getProductImage(sanitizedStoreCode, sanitizedProductCode, new StringBuilder().append(sanitizedImageName).append(".").append(sanitizedExtension).toString(), size);
		} catch (ServiceException e) {
			LOGGER.error("Cannot retrieve image or invalid parameters", e);
		}
		if(image!=null) {
			return image.getFile().toByteArray();
		} else {
			//empty image placeholder
			return tempImage;
		}

	}
	
	/**
	 * Exclusive method for dealing with product images
	 * @param storeCode
	 * @param productCode
	 * @param imageName
	 * @param extension
	 * @param request
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/static/products/{storeCode}/{productCode}/{imageSize}/{imageName}.{extension}",
			produces = {"image/gif", "image/jpg", "image/png", "application/octet-stream"})
	public @ResponseBody byte[] printImage(@PathVariable final String storeCode, @PathVariable final String productCode, @PathVariable final String imageSize, @PathVariable final String imageName, @PathVariable final String extension, HttpServletRequest request) throws IOException {

		// product image small
		// example small product image -> /static/products/DEFAULT/TB12345/SMALL/product1.jpg
		
		// example large product image -> /static/products/DEFAULT/TB12345/LARGE/product1.jpg


		/**
		 * List of possible imageType
		 * 
		 */
		
		// Validate and sanitize all path parameters to prevent path traversal (CWE-022)
		OutputContentFile image = null;
		try {
			String sanitizedStoreCode = sanitizePathParameter(storeCode);
			String sanitizedProductCode = sanitizePathParameter(productCode);
			String sanitizedImageSize = sanitizePathParameter(imageSize);
			String sanitizedImageName = sanitizePathParameter(imageName);
			String sanitizedExtension = sanitizePathParameter(extension);
			
			ProductImageSize size = ProductImageSize.SMALL;
			
			if(FileContentType.PRODUCTLG.name().equals(sanitizedImageSize)) {
				size = ProductImageSize.LARGE;
			} 
			
	

			
			image = productImageService.getProductImage(sanitizedStoreCode, sanitizedProductCode, new StringBuilder().append(sanitizedImageName).append(".").append(sanitizedExtension).toString(), size);
			image = productImageService.getProductImage(sanitizedStoreCode, sanitizedProductCode, new StringBuilder().append(sanitizedImageName).append(".").append(sanitizedExtension).toString(), size);
		} catch (ServiceException e) {
			LOGGER.error("Cannot retrieve image or invalid parameters", e);
		}
		if(image!=null) {
			return image.getFile().toByteArray();
		} else {
			//empty image placeholder
			return tempImage;
		}

	}
	
	/**
	 * Exclusive method for dealing with product images
	 * @param storeCode
	 * @param productCode
	 * @param imageName
	 * @param extension
	 * @param request
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value="/static/products/{storeCode}/{productCode}/{imageName}.{extension}",
	produces = {"image/gif", "image/jpg", "image/png", "application/octet-stream"})
	public @ResponseBody byte[] printImage(@PathVariable final String storeCode, @PathVariable final String productCode, @PathVariable final String imageName, @PathVariable final String extension, HttpServletRequest request) throws IOException {

		// product image
		// example small product image -> /static/products/DEFAULT/TB12345/product1.jpg?size=small
		
		// example large product image -> /static/products/DEFAULT/TB12345/product1.jpg
		// or
		//example large product image -> /static/products/DEFAULT/TB12345/product1.jpg?size=large
		

		/**
		 * List of possible imageType
		 * 
		 */
		
		// Validate and sanitize all path parameters to prevent path traversal (CWE-022)
		OutputContentFile image = null;
		try {
			String sanitizedStoreCode = sanitizePathParameter(storeCode);
			String sanitizedProductCode = sanitizePathParameter(productCode);
			String sanitizedImageName = sanitizePathParameter(imageName);
			String sanitizedExtension = sanitizePathParameter(extension);

			ProductImageSize size = ProductImageSize.LARGE;
			
					
			if(StringUtils.isNotBlank(request.getParameter("size"))) {
				String requestSize = request.getParameter("size");
				if(requestSize.equals(ProductImageSize.SMALL.name())) {
					size = ProductImageSize.SMALL;
				} 
			}
			

			
			image = productImageService.getProductImage(sanitizedStoreCode, sanitizedProductCode, new StringBuilder().append(sanitizedImageName).append(".").append(sanitizedExtension).toString(), size);
		} catch (ServiceException e) {
			LOGGER.error("Cannot retrieve image or invalid parameters", e);
		}
		if(image!=null) {
			return image.getFile().toByteArray();
		} else {
			//empty image placeholder
			return tempImage;
		}

	}

}
