package lq.hh.resume;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lq.hh.exception.CannotUpdateException;
import lq.hh.exception.NoPropertiesException;
import lq.hh.resume.auth.ClientIdentity;
import lq.hh.resume.auth.secret.SecretManager;
import lq.hh.resume.auth.token.PropertiesFileTokenRepository;
import lq.hh.resume.auth.token.SeleniumTokenLoader;
import lq.hh.resume.auth.token.TokenLoader;
import lq.hh.resume.auth.token.TokenRepository;
import lq.hh.resume.services.HttpService;
//
public class ResumeUpdater implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResumeUpdater.class);
    
	private SecretManager secretManager;
	private TokenRepository tokenRepository;
	private TokenLoader tokenLoader;
	private int NUMBER_OF_FETCH_TOKEN_ATTEMPTS = 1;
	private ClientIdentity identity;
	
	public ResumeUpdater() {
		 secretManager = new SecretManager();
		 tokenRepository = new PropertiesFileTokenRepository();
		 identity = secretManager.getClientIdentity();
		 secretManager.storeClientIdentity(identity);


		 tokenLoader = new SeleniumTokenLoader(identity, NUMBER_OF_FETCH_TOKEN_ATTEMPTS);
	}
	
	public void run() {
		
		boolean exit = false;
		while( !exit){
			try {
				String token = tokenRepository.load();
				try{
					if (token != null && !token.isEmpty()) {
						updateResume(token);
						logger.info("Итерация обновления резюме завершена успешно");
						exit = true;
					} else {
						throw new CannotUpdateException("Токен неверный, либо не получен");
					}
				} catch(CannotUpdateException e){
					logger.error("Can't update..", e);
					token = tokenLoader.load();
					
					logger.info("Store TOKEN in properties. It expired at some weeks ago. " + token);
					tokenRepository.save(token);
					
					updateResume(token);
					exit = true;
				}
			} catch (Exception e1) {
				logger.error(e1.getLocalizedMessage());
			}
		} //end while
	}
	
	/**
	 * Использует полученный токен для обновления резюме посредством API hh.ru 
	 * @param accessToken
	 * @throws CannotUpdateException
	 */
	private void updateResume(String accessToken) throws CannotUpdateException {
		try {
			String jsonResumes = HttpService.get("/resumes/mine", accessToken);

			JSONObject resumes = new JSONObject(jsonResumes);
			JSONArray items = resumes.getJSONArray("items");
			if (items.length() > 0) {
				JSONObject firstResume = items.getJSONObject(0);
				String resumeId = firstResume.getString("id");
				logger.info("Try update resume: " + resumeId);

				// update resume
				HttpService.post("/resumes/" + resumeId + "/publish", accessToken);
			}
		} catch (Exception e) {
			throw new CannotUpdateException("Exception when handle resume information", e);
		}
	}
 
	
	public static void main(String[] args) throws Exception {
		testSystemPropertiesAvailability();
		(new ResumeUpdater()).run();
	}
	
	private static void testSystemPropertiesAvailability() {
        logger.debug(System.getProperty("webdriver.chrome.driver"));
        assert System.getProperty("webdriver.chrome.driver") != null;
	}
}
