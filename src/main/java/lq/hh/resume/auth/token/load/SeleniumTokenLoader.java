package lq.hh.resume.auth.token.load;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lq.hh.exception.CannotGetAuthorizationCodeException;
import lq.hh.exception.CannotUpdateException;
import lq.hh.exception.NullPathToChromeExecutable;
import lq.hh.exception.WrongPathToChromeExecutable;
import lq.hh.resume.ResumeUpdater;
import lq.hh.resume.auth.entity.ClientIdentity;
import lq.hh.resume.services.VariablesService;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SeleniumTokenLoader implements TokenLoader {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumTokenLoader.class);
    private static final Integer JETTY_PORT = 8090;
	private static final String HINT_CHROME_BINARY_URL = "https://googlechromelabs.github.io/chrome-for-testing/#stable";
	private ClientIdentity identity;
	private static String AUTH_LOCATION = "https://hh.ru/oauth/authorize";
	private static final String WEB_DRIVER_HEADLESS = "WEB_DRIVER_HEADLESS";
	private String code = null;
	private int numberOfAttempts;
	private boolean success;
	private String chromeDriverBinaryPath;
	private static final int ATTEMPT_TIMEOUT = 7000;
	private boolean headless;

	public SeleniumTokenLoader(ClientIdentity identity, int numberOfAttempts, String chromeDriverBinaryPath, VariablesService variablesService) {
		this.identity = identity;
		this.numberOfAttempts = numberOfAttempts;
		this.chromeDriverBinaryPath = chromeDriverBinaryPath;
		this.headless = variablesService.getBoolean(WEB_DRIVER_HEADLESS, true);
	}
	
	@Override
	public String load() {
		int count = 1;
		success = false;
		while (!success) {
			try {
				loadAuthorizationCode(new LoadCallback() {
					@Override
					public void run(String c) {
						code = c;
						success = true;
					}
				});
			} catch (WrongPathToChromeExecutable e){
				logger.error("Please download latest chrome binary for your 0perational System from {} and set env var {}", HINT_CHROME_BINARY_URL, ResumeUpdater.CHROME_BINARY);
				throw e;
			} catch (NullPathToChromeExecutable e){
				logger.error("Please download latest chrome binary for your 0perational System from {} and set env var {}", HINT_CHROME_BINARY_URL, ResumeUpdater.CHROME_BINARY);
				throw e;
			} catch (Exception e) {
				logger.info("try " + count + " of " + numberOfAttempts);
				if (count++ >= numberOfAttempts) {
					throw e;
				}
			}
			try {
				TimeUnit.MILLISECONDS.sleep(ATTEMPT_TIMEOUT);
			} catch (InterruptedException e) {
				logger.error("", e);
			}
		} // end while
		return fetchAccessToken(code);
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
	public String fetchAccessToken(String code) {
		String accessToken = null;
	
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
			OAuthJSONAccessTokenResponse jsonResponse;
				jsonResponse = urlConnectionClient.execute(request, headers, "POST",
						OAuthJSONAccessTokenResponse.class);

			logger.debug(jsonResponse.getBody());
			Map<String, Object> jsonMap = JSONUtils.parseJSON(jsonResponse.getBody());
			if (jsonMap.containsKey("access_token")) {
				accessToken = String.valueOf(jsonMap.get("access_token"));
				logger.info("Access token: " + accessToken);
			}
			;
		} catch (OAuthProblemException | OAuthSystemException e) {
			throw new CannotUpdateException(e); 
		}
		return accessToken;
	}
	

	private Server startCatchServer(final LoadCallback callback) throws CannotUpdateException{
		
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
						logger.error("", e1);
					}
					try {
						hhResponseCatcher.stop();

						logger.info("Jetty server stopped");
					} catch (Exception e) {
						logger.error("Error when stop Jetty server!", e);
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
	
	

	
	public  void loadAuthorizationCode(LoadCallback callback) throws CannotUpdateException {
		
		//jetty сервер ловит ответы от hh.ru, анализирует переданный код
		//и выполняет необходимые действия по авторизации
		Server hhResponseCatcher = null;
		try{
			hhResponseCatcher = startCatchServer(callback);
			authorizeWithSeleniumDriver();
		} catch(OAuthSystemException e){
			throw new CannotGetAuthorizationCodeException(e);
		} finally {
			if (hhResponseCatcher != null && hhResponseCatcher.isStarted()) {
				try {
					 hhResponseCatcher.stop();
					 logger.error("Response Catcher stopped by timeout. No response from HH arrived. Please investigate." );
				} catch (final Exception e) {
					 logger.error("Exception when stopping response catch service", e);
				}
			}
		}
	}

	private static void delaySec(int timeout) {
		try {
			TimeUnit.SECONDS.sleep(timeout);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
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
	private void authorizeWithSeleniumDriver() throws OAuthSystemException{

		//в системе нужно установить chrome driver. если он не установлен глобально, то необходимо скачать и настроить путь к нему

		logger.info("Identity: " + identity);
		OAuthClientRequest request = OAuthClientRequest.authorizationLocation(AUTH_LOCATION)
				.setClientId(identity.getClientId()).setResponseType("code").buildQueryMessage();

		logger.info("Emulate user login and fetch ACCESS_TOKEN");
		ChromeOptions options = new ChromeOptions();
		if(chromeDriverBinaryPath != null) {
			try {

				options.setBinary(chromeDriverBinaryPath);
			} catch (IllegalArgumentException ie) {
				throw new NullPathToChromeExecutable(chromeDriverBinaryPath, ie);
			}
		}

		if(headless) {
			options.addArguments("--headless=new");
		}
		options.setPageLoadTimeout(Duration.of(15, ChronoUnit.SECONDS));
		WebDriver driver = new ChromeDriver(options);

		logger.info("driver location: " + request.getLocationUri());
		driver.get(request.getLocationUri());
		logger.info("Fill authorization form at hh.ru with selenium");
		WebElement userNameField = driver.findElement(new By.ByXPath("//input[@data-qa='login-input-username']"));
		String userName = identity.getHhUserName();
		userNameField.sendKeys(userName);
		WebElement passwordField = driver.findElement(new By.ByXPath("//input[@data-qa='login-input-password']"));
		String password = identity.getHhPassword();
		passwordField.sendKeys(password);

		WebElement sendButton = driver.findElement(new By.ByXPath("//button[@data-qa='account-login-submit']"));
		logger.info("Submit form for token");

		sendButton.submit();
		delaySec(2);

		logger.info("Stopping selenium... Server's answer with generated token expected in callback.");
		driver.close(); // close selenium window
		driver.quit();
	}
}
