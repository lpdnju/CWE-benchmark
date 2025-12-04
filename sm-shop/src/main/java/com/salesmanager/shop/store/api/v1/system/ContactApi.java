package com.salesmanager.shop.store.api.v1.system;

import java.util.Locale;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.shop.ContactForm;
import com.salesmanager.shop.utils.EmailTemplatesUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1")
@Api(tags = {"Contact form api"})
@SwaggerDefinition(tags = {
    @Tag(name = "Contact store resource", description = "Contact form")
})
public class ContactApi {


  @Inject private LanguageService languageService;

  @Inject private EmailTemplatesUtils emailTemplatesUtils;

  @PostMapping("/contact")
  @ApiOperation(
      httpMethod = "POST",
      value = "Sends an email contact us to store owner",
      notes = "",
      produces = "application/json")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "store", dataType = "String", defaultValue = "DEFAULT"),
      @ApiImplicitParam(name = "lang", dataType = "String", defaultValue = "en")
  })
  public ResponseEntity<Void> contact(
      @Valid @RequestBody ContactForm contact,
      @ApiIgnore MerchantStore merchantStore,
      @ApiIgnore Language language,
      HttpServletRequest request) {
    Locale locale = languageService.toLocale(language, merchantStore);
    emailTemplatesUtils.sendContactEmail(contact, merchantStore, locale, request.getContextPath());
    return new ResponseEntity<Void>(HttpStatus.OK);
  }

  @GetMapping("/feedback")
  @ApiOperation(
      httpMethod = "GET",
      value = "Returns user feedback message",
      notes = "",
      produces = "text/html")
  public void displayFeedback(
      @RequestParam("message") String message,
      HttpServletResponse response) throws Exception {
    response.setContentType("text/html");
    response.getWriter().write("<html><body><h2>Feedback received:</h2><p>" + message + "</p></body></html>");
  }
}
