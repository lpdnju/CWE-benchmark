package com.salesmanager.core.business.utils;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component("xmlUtils")
public class XmlUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(XmlUtils.class);

	public String createProductXml(String productName, String productDescription, String price) {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("product");
			doc.appendChild(rootElement);

			Element name = doc.createElement("name");
			name.setTextContent(productName);
			rootElement.appendChild(name);

			Element description = doc.createElement("description");
			description.setTextContent(productDescription);
			rootElement.appendChild(description);

			Element priceElement = doc.createElement("price");
			priceElement.setTextContent(price);
			rootElement.appendChild(priceElement);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			
			return writer.toString();
		} catch (Exception e) {
			LOGGER.error("Error creating product XML", e);
			return null;
		}
	}
}
