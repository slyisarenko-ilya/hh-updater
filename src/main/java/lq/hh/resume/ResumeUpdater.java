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
//
public class ResumeUpdater implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResumeUpdater.class);

    private static final Integer JETTY_PORT = 8090;
    
	private static final String TOKEN = "token";

	private static final int FETCH_TOKEN_TRIES_COUNT = 0;
	private static String AUTH_LOCATION = "https://hh.ru/oauth/authorize";
	
	private SecretManager secretManager;
	
	public ResumeUpdater() {
		 secretManager = new SecretManager();
	}
	
	/**
	 * Приложение делает сервер-сервер POST-запрос на
	 * https://m.hh.ru/oauth/token для обмена полученного authorization_code
	 * на access_token. В запросе необходимо передать:
	 * grant_type=authorization_code&client_id={CLIENT_ID}&client_secret
	 * ={CLIENT_SECRET}&code={CODE} Тело запроса необходимо передавать в
	 * стандартном application/x-www-form-urlencoded с указанием
	 * соответствующего заголовка Content-Type.
	 * 
	 */
	public String fetchAccessToken(String code) throws OAuthProblemException, NoPropertiesException {
		String accessToken = null;

		ClientIdentity identity = secretManager.getClientIdentity();
		
		String clientId = identity.getClientId();
		String clientSecret = identity.getClientSecret();

		logger.debug("\n[STEP] Request Access Token");
		OAuthClientRequest request = null;
		try {
			
			request = OAuthClientRequest.tokenLocation("https://hh.ru/oauth/token/")
					.setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(clientId).setClientSecret(clientSecret)
					.setCode(code).buildBodyMessage();

			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", "application/x-www-form-urlencoded");

			// create OAuth client that uses custom http client under the hood
			URLConnectionClient urlConnectionClient = new URLConnectionClient();
			OAuthJSONAccessTokenResponse jsonResponse = urlConnectionClient.execute(request, headers, "POST",
					OAuthJSONAccessTokenResponse.class);

			logger.debug(jsonResponse.getBody());
			Map<String, Object> jsonMap = JSONUtils.parseJSON(jsonResponse.getBody());
			if (jsonMap.containsKey("access_token")) {
				accessToken = String.valueOf(jsonMap.get("access_token"));
				logger.info("Access token: " + accessToken);
			}
			;

		} catch (OAuthSystemException e) {
			logger.error(e.getLocalizedMessage());
		}
		return accessToken;
	}
	

	public String getContent(HttpURLConnection con) throws IOException {
		InputStream inputStream = con.getInputStream();
		StringBuffer html = null;
		if (inputStream != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
			if (in != null) {
				html = new StringBuffer();
				char[] buffer = new char[2000];

				while (in.read(buffer) > 0) {
					html.append(buffer);
				}
				;

				in.close();
			}
		}
		inputStream.close();
		return String.valueOf(html);
	}

	/**
	 * Использует полученный токен для обновления резюме посредством API hh.ru 
	 * @param accessToken
	 * @throws CannotUpdateException
	 */
	private void updateResume(String accessToken) throws CannotUpdateException {
		try {
			String jsonResumes = HttpUtil.get("/resumes/mine", accessToken);

			JSONObject resumes = new JSONObject(jsonResumes);
			JSONArray items = resumes.getJSONArray("items");
			if (items.length() > 0) {
				JSONObject firstResume = items.getJSONObject(0);
				String resumeId = firstResume.getString("id");
				logger.info("Try update resume: " + resumeId);

				// update resume
				HttpUtil.post("/resumes/" + resumeId + "/publish", accessToken);
			}
		} catch (Exception e) {
			throw new CannotUpdateException("Exception when handle resume information", e);
		}
	}
 
	
	private Server startCatchServer(final CodeCallback callback) throws CannotUpdateException{
		
		final Server hhResponseCatcher = new Server(JETTY_PORT);
		/*
		 * Если пользователь не разрешает доступ приложению, мы
		 * перенаправляем пользователя на указанный redirect_uri с
		 * ?error=access_denied и state={STATE}, если таковой был указан при
		 * первом запросе. Иначе в редиректе мы указываем временный
		 * authorization_code: 
		 */
		Handler callbackHandler = new AbstractHandler() {
			
			public void handle(String target, Request baseRequest, HttpServletRequest request,
					HttpServletResponse response) throws IOException, ServletException {
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				if (target.contains("/code")) {
					String code = baseRequest.getParameter("code");
					if (code != null && !code.isEmpty()) {
						logger.info("Work with code: " + target + ": " + code);

						response.setStatus(HttpServletResponse.SC_OK);
						baseRequest.setHandled(true);
						response.getWriter().append("Token fetched");
					}

					logger.info("Get token and try update resume");
					try {
						callback.run(code);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					try {
						hhResponseCatcher.stop();

						logger.info("Jetty server stopped");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						logger.error("Error when stop Jetty server!" + e.getLocalizedMessage());
					}
					hhResponseCatcher.destroy();

				}
				response.setContentType("text/html;charset=utf-8");
			}
		};

		hhResponseCatcher.setHandler(callbackHandler);
		try{
			hhResponseCatcher.start();
		} catch(Exception e){
			e.printStackTrace();
			throw new CannotUpdateException("Cannot start server jetty", e);
		}
		logger.info("Server started");
		
		return hhResponseCatcher;
	}
	
	
	public  void retrieveAuthorizationCodeAndUpdateResume(final CodeCallback callback) throws CannotUpdateException, NoPropertiesException {
		//jetty сервер ловит ответы от hh.ru, анализирует переданный код
		//и выполняет необходимые действия по авторизации
		Server hhResponseCatcher = null;
		try{
			hhResponseCatcher = startCatchServer(callback);
			authorizeWithSeleniumDriver();
		}catch(NoPropertiesException npe){//
			throw npe;
		} catch(Exception e){
			logger.error(e.getLocalizedMessage());
			throw new CannotUpdateException("Cannot start selenium browser", e);
		} finally {
			if (hhResponseCatcher != null && hhResponseCatcher.isStarted()) {
				try {
					 hhResponseCatcher.stop();
					 logger.error("Server stopped after error. Try remove or correct properties file or investigate problem depeer" );
				} catch (final Exception e) {
					e.printStackTrace();
					throw new CannotUpdateException("jetty problems", e);
				}
			}
		}
	}

	
	/**
	 * Приложение направляет пользователя по адресу:
	 * https://m.hh.ru/oauth/authorize?response_type=code&client_id={
	 * CLIENT_ID}&state={STATE}. Параметр state опционален, в случае его
	 * указания, он будет включен в ответный редирект — После
	 * прохождения авторизации на сайте, мы запрашиваем у пользователя
	 * разрешение доступа приложения к его персональным данным.
	 */
	private void authorizeWithSeleniumDriver() throws OAuthSystemException, NoPropertiesException{
		
		//в системе нужно установить chrome driver. если он не установлен глобально, то необходимо скачать и настроить путь к нему
		//System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir")+"/chromedriver");
		
		ClientIdentity identity = secretManager.getClientIdentity();
		logger.info("Identity: " + identity);
		OAuthClientRequest request = OAuthClientRequest.authorizationLocation(AUTH_LOCATION)
				.setClientId(identity.getClientId()).setResponseType("code").buildQueryMessage();

		logger.info("Emulate user login and fetch ACCESS_TOKEN");
		WebDriver driver = new ChromeDriver();
		logger.info("driver location: " + request.getLocationUri());
		driver.get(request.getLocationUri());
		logger.info("Fill authorization form at hh.ru with selenium");

		WebElement userNameField = driver.findElement(By.ByName.name("username"));
		String userName = identity.getHhUserName();
		userNameField.sendKeys(userName);
		WebElement passwordField = driver.findElement(By.ByName.name("password"));
		String password = identity.getHhPassword();
		passwordField.sendKeys(password);
		WebElement sendButton = driver.findElement(By.ByCssSelector.cssSelector("input[type='submit']"));
		sendButton.submit();
		logger.info("Stopping selenium...");
		driver.close(); // close selenium window
		driver.quit();
	}


	public  void fullTokenUpdateProcess() throws Exception {
		logger.info("Try update resume");
		retrieveAuthorizationCodeAndUpdateResume(new CodeCallback() {
			@Override
			public void run(String code) throws Exception {
				String accessToken = fetchAccessToken(code);
				logger.info("Store TOKEN in properties. It expired at some weeks ago. " + accessToken);
				PropertiesService.storeProperty(TOKEN, accessToken);
				updateResume(accessToken);
			}
		});
	}

	
	/**
	 * Несколько раз пытается обновить токен. 
	 * В случае неудачи выбрасывает исключение.
	 * @throws CannotUpdateException
	 */
	public  void tryFullTokenUpdateProcess() throws CannotUpdateException {
		int count = 1;
		boolean success = false;
		while (!success) {
			try {
				fullTokenUpdateProcess();
				success = true;
			} catch (Exception e) {
				logger.info("try " + count + " of " + FETCH_TOKEN_TRIES_COUNT);
				if (count++ > FETCH_TOKEN_TRIES_COUNT) {
					throw new CannotUpdateException("Не удалось обновить резюме", e);
				}
			}
		} // end while
	}


	public void run() {

		boolean updated = false; // флаг, означающий факт успешного обновления резюме
									
		boolean propertiesExists = false;
		boolean exit = false;
		while(!propertiesExists && !exit){
			try {
	
				String token = PropertiesService.loadProperty(TOKEN);
				propertiesExists = true;
				try{
					if (token != null && !token.isEmpty()) {
						updateResume(token);
						updated = true;
					} else {
						throw new CannotUpdateException("Токен неверный, либо не получен");
					}
				} catch(CannotUpdateException e){
					logger.error(e.getLocalizedMessage());
				}
				if(updated){
					logger.info("Итерация обновления резюме завершена успешно");
				} else {
					tryFullTokenUpdateProcess();
				}
			} catch(NoPropertiesException npe){
				propertiesExists = false;
				try{
					buildPropertiesInteractive();
				} catch(IOException ioe){ // в случае такой ошибки - завершать процесс.
					
					logger.error("Не получается сохранить параметры. Здесь уже врядли что-то поможет кроме разработчика.", ioe);
					exit = true;
					
				}
			} catch (Exception e1) {
				logger.error(e1.getLocalizedMessage());
			}
		} //end while
	}

	
	public void buildPropertiesInteractive() throws IOException{
		ClientIdentity identity = secretManager.getClientIdentity();
		secretManager.storeClientIdentity(identity);
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
