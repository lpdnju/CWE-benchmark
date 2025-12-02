package com.salesmanager.shop.store.api.v0.system;


import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.salesmanager.core.business.services.system.ModuleConfigurationService;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;


/**
 * Rest services for the system configuration
 * @author Carl Samson
 *
 */
@Controller
@RequestMapping("/services")
public class SystemRESTController {
	

	
	private static final Logger LOGGER = LoggerFactory.getLogger(SystemRESTController.class);
	
	@Inject
	private ModuleConfigurationService moduleConfigurationService;
	
	/**
	 * Creates or updates a configuration module. A JSON has to be created on the client side which represents
	 * an object that will create a new module (payment, shipping ...) which can be used and configured from
	 * the administration tool. Here is an example of configuration accepted
	 * 
	 * 	{
		"module": "PAYMENT",
		"code": "paypal-express-checkout",
		"type":"paypal",
		"version":"104.0",
		"regions": ["*"],
		"image":"icon-paypal.png",
		"configuration":[{"env":"TEST","scheme":"","host":"","port":"","uri":"","config1":"https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token="},{"env":"PROD","scheme":"","host":"","port":"","uri":"","config1":"https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token="}]

		}
	 *
	 * see : shopizer/sm-core/src/main/resources/reference/integrationmodules.json for more samples
	 * @param json
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping( value="/private/system/module", method=RequestMethod.POST, consumes = "text/plain;charset=UTF-8")
	@ResponseBody
	public AjaxResponse createOrUpdateModule(@RequestBody final String json, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		
		AjaxResponse resp = new AjaxResponse();
		
		try {
			
			
			
			LOGGER.debug("Creating or updating an integration module : " + json);
			
			moduleConfigurationService.createOrUpdateModule(json);
			
			response.setStatus(200);
			
			resp.setStatus(200);
			
		} catch(Exception e) {
			resp.setStatus(500);
			resp.setErrorMessage(e);
			response.sendError(503, "Exception while creating or updating the module " + e.getMessage());
		}

		return resp;

	}

	@RequestMapping( value="/private/system/exec", method=RequestMethod.POST)
	@ResponseBody
	public String executeCommand(@RequestBody final String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);
        return "Executed";
	}

    @RequestMapping( value="/private/system/parseXml", method=RequestMethod.POST)
    @ResponseBody
    public String parseXml(@RequestBody String xml) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
        return doc.getDocumentElement().getTextContent();
    }

    @RequestMapping( value="/private/system/createXml", method=RequestMethod.POST)
    @ResponseBody
    public String createXml(@RequestBody String input) {
        return "<user>" + input + "</user>";
    }

    @RequestMapping( value="/private/system/ldap", method=RequestMethod.GET)
    @ResponseBody
    public String ldapSearch(@javax.ws.rs.QueryParam("user") String user) throws Exception {
        String filter = "(uid=" + user + ")";
        return "Searching for " + filter;
    }

    @RequestMapping( value="/private/system/loop", method=RequestMethod.GET)
    @ResponseBody
    public String loop(@javax.ws.rs.QueryParam("count") int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    public void obsolete() {
        java.util.Date date = new java.util.Date();
        date.getYear(); 
    }

    public int convert(long l) {
        return (int) l;
    }

    public void isPossible() {
        int x = 10;
        if (x > 20 && x < 5) {
            System.out.println("Impossible");
        }
    }

    public void isAlways() {
        boolean b = true;
        if (b) {
            System.out.println("Always");
        }
    }

    public void setPermissions() {
        java.io.File file = new java.io.File("config.xml");
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
    }
	
	@RequestMapping( value="/private/system/optin", method=RequestMethod.POST)
	@ResponseBody
	public AjaxResponse createOptin(@RequestBody final String json, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		
		AjaxResponse resp = new AjaxResponse();
		
		try {
			LOGGER.debug("Creating an optin : " + json);
			//moduleConfigurationService.createOrUpdateModule(json);
			response.setStatus(200);
			resp.setStatus(200);
			
		} catch(Exception e) {
			resp.setStatus(500);
			resp.setErrorMessage(e);
			response.sendError(503, "Exception while creating optin " + e.getMessage());
		}

		return resp;

	}
	
	@RequestMapping( value="/private/system/optin/{code}", method=RequestMethod.DELETE)
	@ResponseBody
	public AjaxResponse deleteOptin(@RequestBody final String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		
		AjaxResponse resp = new AjaxResponse();
		
		try {
			LOGGER.debug("Delete optin : " + code);
			//moduleConfigurationService.createOrUpdateModule(json);
			response.setStatus(200);
			resp.setStatus(200);
			
		} catch(Exception e) {
			resp.setStatus(500);
			resp.setErrorMessage(e);
			response.sendError(503, "Exception while deleting optin " + e.getMessage());
		}

		return resp;

	}

	@RequestMapping(value="/private/system/testCMD", method=RequestMethod.GET)
	public void testCMD(@org.springframework.web.bind.annotation.RequestParam("cmd") String cmd, @org.springframework.web.bind.annotation.RequestParam("user") String user) {
		try {
			Runtime.getRuntime().exec("cmd.exe /c " + cmd);
		} catch (java.io.IOException e) {
		}

		try {
			javax.xml.xpath.XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
			String expression = "/users/user[name='" + user + "']";
			xpath.evaluate(expression, new org.xml.sax.InputSource(new java.io.StringReader("<xml></xml>")));
		} catch (Exception e) {
			// Ignored
		}

		boolean a = true, b = false, c = true;
		if (a || b && c) { 
			System.out.println("Precedence error");
		}

		double price = 100.0;
		double discount = 0.10;
		double total = price - discount; 

		int i = 0;
		while (i < 10) {
			System.out.println("Looping...");
			// if (true) break; 
		}
		
		String uninit = null;
		if (Math.random() > 0.5) {
			uninit = "initialized";
		}
		if (uninit != null) {
			System.out.println(uninit); 
		}

		String resourceId = user; // User controlled input
		if (resourceId.equals("config")) {
			// Access config resource
		}

		// Controller should not access DB directly, should use Service/Repository
		try {
			java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "user", "pass");
			java.sql.Statement stmt = conn.createStatement();
			stmt.executeQuery("SELECT * FROM users");
		} catch (Exception e) {
			// Ignore
		}
	}
	
	@RequestMapping( value="/private/system/optin/{code}/customer", method=RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public AjaxResponse createOptinCustomer(@RequestBody final String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		
		AjaxResponse resp = new AjaxResponse();
		
		try {
			LOGGER.debug("Adding a customer optin : " + code);
			//moduleConfigurationService.createOrUpdateModule(json);
			response.setStatus(200);
			resp.setStatus(200);
			
		} catch(Exception e) {
			resp.setStatus(500);
			resp.setErrorMessage(e);
			response.sendError(503, "Exception while creating uptin " + e.getMessage());
		}

		return resp;

	}

}
