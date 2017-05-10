package lq.hh.resume;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
//
public class ResumeUpdater implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResumeUpdater.class);

	private static final String propertiesPath = "./updater.properties";
	private static final String TOKEN = "token";
	private static final String CLIENT_SECRET = "client_secret";
	private static final String CLIENT_ID = "client_id";
	private static final String HEADHUNT_USERNAME = "username";
	private static final String HEADHUNT_PASSWORD = "password";
		
	
	public static String fetchAccessToken(String code) throws OAuthProblemException, NoPropertiesException {
		String accessToken = null;


		/*
		 * Приложение делает сервер-сервер POST-запрос на
		 * https://m.hh.ru/oauth/token для обмена полученного authorization_code
		 * на access_token. В запросе необходимо передать:
		 * grant_type=authorization_code&client_id={CLIENT_ID}&client_secret
		 * ={CLIENT_SECRET}&code={CODE} Тело запроса необходимо передавать в
		 * стандартном application/x-www-form-urlencoded с указанием
		 * соответствующего заголовка Content-Type.
		 * 
		 */
		String clientId = loadProperty(CLIENT_ID);
		String clientSecret = loadProperty(CLIENT_SECRET);

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
			// TODO Auto-generated catch block
			logger.error(e.getLocalizedMessage());
		}
		return accessToken;
	}

	public static String getContent(HttpURLConnection con) throws IOException {
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

	private static void updateResume(String accessToken) throws CannotUpdateException {
		try {
			String jsonResumes = get("/resumes/mine", accessToken);

			JSONObject resumes = new JSONObject(jsonResumes);
			JSONArray items = resumes.getJSONArray("items");
			if (items.length() > 0) {
				JSONObject firstResume = items.getJSONObject(0);
				String resumeId = firstResume.getString("id");
				logger.info("Попытка обновления даты резюме с номером: " + resumeId);

				// update resume
				post("/resumes/" + resumeId + "/publish", accessToken);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new CannotUpdateException("Исключение при получении\\обработке данных резюме", e);
		}

	}

	@SuppressWarnings("finally")
	public static void retrieveAuthorizationCodeAndUpdateResume(final CodeCallback callback) throws CannotUpdateException, NoPropertiesException {
		// TODO Auto-generated method stub
		final Server server = new Server(8090);
		try {
			/*
			 * Если пользователь не разрешает доступ приложению, мы
			 * перенаправляем пользователя на указанный redirect_uri с
			 * ?error=access_denied и state={STATE}, если таковой был указан при
			 * первом запросе. Иначе в редиректе мы указываем временный
			 * authorization_code:
			 */
			Handler h = new AbstractHandler() {
				public void handle(String target, Request baseRequest, HttpServletRequest request,
						HttpServletResponse response) throws IOException, ServletException {
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					if (target.contains("/code")) {
						String code = baseRequest.getParameter("code");
						if (code != null && !code.isEmpty()) {
							logger.info("Работаем с кодом: " + target + ": " + code);

							response.setStatus(HttpServletResponse.SC_OK);
							baseRequest.setHandled(true);
							response.getWriter().append("Токен успешно получен");
						}

						logger.info("Получаем токен и пытаемся обновить резюме");
						try {
							callback.run(code);
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							server.stop();

							logger.info("Jetty server stopped");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							logger.error("Error when stop Jetty server!" + e.getLocalizedMessage());
						}
						server.destroy();

					}
					response.setContentType("text/html;charset=utf-8");
				}
			};

			server.setHandler(h);
			try{
			server.start();
			} catch(Exception e){
				e.printStackTrace();
				throw new CannotUpdateException("Не удалось запустить сервер jetty", e);
			}
			logger.info("Server started");

			/*
			 * Приложение направляет пользователя по адресу:
			 * https://m.hh.ru/oauth/authorize?response_type=code&client_id={
			 * CLIENT_ID}&state={STATE}. Параметр state опционален, в случае его
			 * указания, он будет включен в ответный редирект — После
			 * прохождения авторизации на сайте, мы запрашиваем у пользователя
			 * разрешение доступа приложения к его персональным данным.
			 */
			//
			
			OAuthClientRequest request = OAuthClientRequest.authorizationLocation("https://hh.ru/oauth/authorize")
					.setClientId(loadProperty(CLIENT_ID)).setResponseType("code").buildQueryMessage();

//в системе нужно установить chrome driver. если он не установлен глобально, то необходимо скачать и настроить путь к нему
//			System.setProperty("webdriver.chrome.driver", System.getProperty("user.dir")+"/chromedriver");
			
			logger.info("Эмулируем вход пользователя и получаем ACCESS_TOKEN");
			WebDriver driver = new ChromeDriver();
			logger.info("driver location: " + request.getLocationUri());
			driver.get(request.getLocationUri());
			logger.info("Заполняем форму авторизации hh.ru при помощи selenium");

			WebElement userNameField = driver.findElement(By.ByName.name("username"));
			String userName = loadProperty(HEADHUNT_USERNAME);
			userNameField.sendKeys(userName);
			WebElement passwordField = driver.findElement(By.ByName.name("password"));
			String password = loadProperty(HEADHUNT_PASSWORD);
			passwordField.sendKeys(password);
			WebElement sendButton = driver.findElement(By.ByCssSelector.cssSelector("input[type='submit']"));
			sendButton.submit();

			logger.info("Останавливаем selenium");
			driver.close(); // close selenium window
			driver.quit();
		}catch(NoPropertiesException npe){//
			throw npe;
		} catch(Exception e){
			
			e.printStackTrace();
			throw new CannotUpdateException("Не удалось запустить браузер selenium", e);
		} finally {
			if (server != null && server.isStarted()) {
				try {
					 server.stop();
					 logger.error("Сервер остановлен после ошибки. Удалите либо скорректируйте настроечный файл" );

				} catch (final Exception e) {
					e.printStackTrace();
					throw new CannotUpdateException("Проблемы с сервером jetty", e);
				}
  			    finalizeScheduler();	
			}
		}
	}

	public static void testAccessToken(String accessToken) throws ClientProtocolException, IOException {

		HttpGet testRequest = new HttpGet("https://api.hh.ru/me");
		testRequest.setHeader("User-Agent",
				"Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:45.0) Gecko/20100101 Firefox/45.0");
		testRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		HttpEntity entity = response.getEntity();
		// String entityContents = EntityUtils.toString(entity);

	}

	public static String get(String command, String accessToken) throws ClientProtocolException, IOException {
		HttpGet testRequest = new HttpGet("https://api.hh.ru" + command);
		testRequest.setHeader("User-Agent",
				"Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:45.0) Gecko/20100101 Firefox/45.0");
		testRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		HttpEntity entity = response.getEntity();
		String entityContents = EntityUtils.toString(entity);

		return entityContents;
	}

	private static  final Map<Integer, String> responseStatusMessages = new HashMap<Integer, String>(){{
		put(new Integer(429), "Резюме ещё не готово обновиться. Можно раз в 4 часа");
		put(new Integer(204), "Резюме обновлено!");
	}};


	public static void post(String command, String accessToken) throws ClientProtocolException, IOException {
		HttpPost testRequest = new HttpPost("https://api.hh.ru" + command);
		// testRequest.setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux
		// i686; rv:45.0) Gecko/20100101 Firefox/45.0");
		testRequest.setHeader("User-Agent", "Resume updater/1.0");
		testRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		logger.info("Про статусы ответа можете почитать здесь: https://z5h64q92x9.net/proxy_u/ru-en.en/http/hhru.github.io/api/rendered-docs/docs/resumes.md.html#publish");
		logger.info("Ответ сервера в сыром виде: " + ToStringBuilder.reflectionToString(response.getStatusLine(), ToStringStyle.MULTI_LINE_STYLE));
		
		logger.info(responseStatusMessages.get(new Integer(response.getStatusLine().getStatusCode())));

	}


	public static void put(String command, String body, String accessToken)
			throws ClientProtocolException, IOException {

		HttpPut testRequest = new HttpPut("https://api.hh.ru" + command);
		testRequest.setHeader("User-Agent", "Resume updater/1.0");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);
		testRequest.setHeader("Content-Type", "applicatlion/json");

		HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
		logger.info(ToStringBuilder.reflectionToString(entity, ToStringStyle.MULTI_LINE_STYLE));

		testRequest.setEntity(entity);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		entity = response.getEntity();
		String entityContents = EntityUtils.toString(entity);
		logger.info(entityContents);
	}




	public static void storeProperty(String key, String value) {
		try {
			Properties props;
			try{
				props = getProps();
			} catch(NoPropertiesException npe){
				props = new Properties();
			}
			props.setProperty(key, value);
			File f = new File(propertiesPath);
			OutputStream out = new FileOutputStream(f);
			props.store(out, "");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public static Properties getProps() throws NoPropertiesException{
		// First try loading from the current directory
		Properties props = new Properties();
		InputStream is = null;

		try {
			File f = new File(propertiesPath);
			is = new FileInputStream(f);
		} catch (Exception e) {
			is = null;
		}
		try{
			if (is == null) {
				// Try loading from classpath
				is = props.getClass().getResourceAsStream(propertiesPath);
			}
	
			// Try loading properties from the file (if found)
			props.load(is);
		}catch(IOException ioe){
			throw new NoPropertiesException();
		}catch(NullPointerException npe){
			throw new NoPropertiesException();
		}
		return props;
	}
	
	public static String loadProperty(String key) throws NoPropertiesException {
		Properties props = getProps();
		
		String token = props.getProperty(key);
		return token;

	}
	


	public static void fullTokenUpdateProcess() throws Exception {

		// token == null || token.isEmpty()

		logger.info("Try update resume");
		retrieveAuthorizationCodeAndUpdateResume(new CodeCallback() {
			@Override
			public void run(String code) throws Exception {
				String accessToken = fetchAccessToken(code);
				logger.info("Сохраняем TOKEN в настройках. Его хватит на несколько недель. " + accessToken);
				storeProperty(TOKEN, accessToken);
				updateResume(accessToken);
			}
		});
	}

	public static void tryFullTokenUpdateProcess() throws CannotUpdateException {
		int count = 1;
		int maxTries = 3;
		boolean success = false;
		while (!success) {
			try {
				fullTokenUpdateProcess();
				success = true;
			} catch (Exception e) {
				logger.info("try " + count + " of " + maxTries);
				if (count++ > maxTries) {
					throw new CannotUpdateException("Не удалось обновить резюме", e);
				}
			}
		} // end while
	}


	public void run() {

		boolean updated = false; // флаг, означающий что обновление резюме
									// прошло без ошибок
		boolean propertiesExists = false;
		boolean exit = false;
		while(!propertiesExists && !exit){
			try {
	
				String token = loadProperty(TOKEN);
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
				if(!updated){
					tryFullTokenUpdateProcess();
				}
				if(updated){
					logger.info("Итерация обновления резюме завершена успешно");
				}
			} catch(NoPropertiesException npe){
				propertiesExists = false;
				try{
					buildPropertiesInteractive();
				} catch(IOException ioe){ // в случае такой ошибки - завершать процесс.
					
					logger.error("Не получается сохранить параметры. Здесь уже врядли что-то поможет кроме разработчика.");
					ioe.printStackTrace();
					finalizeScheduler();	
					exit = true;
					
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				finalizeScheduler();	 
	
			}
		} //end while
	}

	public void buildPropertiesInteractive() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("CLIENT_ID и CLIENT_SECRET можно получить на https://dev.hh.ru/admin");
        System.out.print("CLIENT_ID:>");
        
        String clientId = br.readLine();
        System.out.print("CLIENT_SECRET:>");
        String clientSecret = br.readLine();
        System.out.print("Логин для авторизации в headhunt:>");
        String userName = br.readLine();
        System.out.print("Пароль headhunt:>");
        String password = br.readLine();
        
        storeProperty(CLIENT_SECRET, clientSecret);
        storeProperty(CLIENT_ID, clientId);
        storeProperty(HEADHUNT_USERNAME, userName);
        storeProperty(HEADHUNT_PASSWORD, password);
        
	}
	
	public static void finalizeScheduler(){
//		scheduler.shutdown();	
//		scheduler = null;	
	}
	
	static ScheduledExecutorService  scheduler;
	
	public static void main(String[] args) throws Exception {

		 (new ResumeUpdater()).run();

//		 scheduler = Executors.newScheduledThreadPool(1);
//		 ResumeUpdater resumeUpdater = new ResumeUpdater();
//		 System.out.println("Update resume scheduler started");
//		 scheduler.scheduleAtFixedRate(resumeUpdater, 0, 60, TimeUnit.MINUTES);
	}

}
