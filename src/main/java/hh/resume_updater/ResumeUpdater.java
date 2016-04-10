package hh.resume_updater;

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
import org.openqa.selenium.firefox.FirefoxDriver;

public class ResumeUpdater 
implements Runnable
 {
	private static final String CLIENT_ID = "G7RLL36FL0Q69S2B8UA6SRE4E16886P551C1O82I1B9VOR27P0ET3IULCAN24SBU";
	private static final String CLIENT_SECRET = "ORKJLA8JPF7T23D4FJALC7F4E7HU8S901H7MB07RNPE44V78OS4T42S40TMMI3DV";

	public static String fetchAccessToken(String code) throws OAuthProblemException {
		String accessToken = null;
		// System.out.println("Fetch access_token by authorization_code (" +
		// code + ")");

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

		// System.out.println("\n[STEP] Request Access Token");
		OAuthClientRequest request = null;
		try {

			request = OAuthClientRequest.tokenLocation("https://hh.ru/oauth/token/")
					.setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(CLIENT_ID).setClientSecret(CLIENT_SECRET)
					.setCode(code).buildBodyMessage();

			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", "application/x-www-form-urlencoded");

			// create OAuth client that uses custom http client under the hood
			URLConnectionClient urlConnectionClient = new URLConnectionClient();
			OAuthJSONAccessTokenResponse jsonResponse = urlConnectionClient.execute(request, headers, "POST",
					OAuthJSONAccessTokenResponse.class);

			// System.out.println(jsonResponse.getBody());
			Map<String, Object> jsonMap = JSONUtils.parseJSON(jsonResponse.getBody());
			if (jsonMap.containsKey("access_token")) {
				accessToken = String.valueOf(jsonMap.get("access_token"));
				// System.out.println(accessToken);
			}
			;

		} catch (OAuthSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	

	private static void getTokenAndUpdateResume(String authorizationCode) throws OAuthProblemException, ClientProtocolException, IOException{
	 String accessToken = fetchAccessToken(authorizationCode);
		 String jsonResumes = get("/resumes/mine", accessToken);
		// System.out.println(jsonResumes);
		
		 JSONObject resumes = new JSONObject(jsonResumes);
		 JSONArray items = resumes.getJSONArray("items");
		 if(items.length() > 0 ){
		 JSONObject firstResume = items.getJSONObject(0);
		 String resumeId = firstResume.getString("id");
		 System.out.println("update resume date " + resumeId);
		 //update resume
		
		 post("/resumes/" + resumeId + "/publish", accessToken);
		// String putBody = "\"salary\": [{\"amount\": 1000000,\"currency\":
//		 \"RUR\" }]";
		// put("/resumes/" + resumeId, putBody, accessToken);
		
		 }
		

	}

	@SuppressWarnings("finally")
	public static void retrieveAuthorizationCodeAndUpdateResume() throws Exception {
		// TODO Auto-generated method stub
		final Server server = new Server(8090);
		String accessToken = null;
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
							System.out.println("handle " + target + ": " + code);
						
							response.setStatus(HttpServletResponse.SC_OK);
							baseRequest.setHandled(true);
							response.getWriter().append("Token successfully got. Now process resume");
						}
						
						try {
						 	System.out.println("Get token and update resume");
							getTokenAndUpdateResume(code);
						} catch (OAuthProblemException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						 try {
						 	server.stop();
						 	
						 	System.out.println("Jetty server stopped");
						 } catch (Exception e) {
						 // TODO Auto-generated catch block
						 	System.out.println("Jetty server stopped!");
							//e.printStackTrace();
						 }
						 server.destroy();
					}
					response.setContentType("text/html;charset=utf-8");
				}
			};

			server.setHandler(h);

			server.start();
			System.out.println("Server started");
			// server.join();

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
					.setClientId(CLIENT_ID)
					.setResponseType("code").buildQueryMessage();

//			request.addHeader("Accept-Encoding", "gzip, deflate, br");
//			request.addHeader("Content-Type", "text/html");
//			request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
//			request.addHeader("Accept-Language", "en-US,en;q=0.5");
//			request.addHeader("Cookie", "hhtoken=_1iu_3NCZs6497YHgowsxAcJG6yy; hhuid=gRzscqtau6_FElbysoIx!Q--;");
//			request.addHeader("Host", "hh.ru");
//			request.addHeader("User-Agent",
//					"Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:45.0) Gecko/20100101 Firefox/45.0");
//			
//			
//			URLConnectionClient client = new URLConnectionClient();
//
//			OAuthClient oAuthClient = new OAuthClient(client);
//						
//			System.out.println("get response");
//			OAuthResourceResponse response = (OAuthResourceResponse) oAuthClient.resource(request, "GET",
//					OAuthResourceResponse.class);
//			System.out.println(response);
//			accessToken = response.getBody();

//  		DesiredCapabilities caps = new DesiredCapabilities();
//  		caps.setJavascriptEnabled(true);  
//  		caps.setCapability(
//            PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
//            "/usr/bin/phantomjs"
//        );
		System.out.println("Initialize web driver");
        WebDriver driver = new  FirefoxDriver();
//        WebDriver driver = new PhantomJSDriver(caps);

        // And now use this to visit Google
        driver.get(request.getLocationUri()); 		
		System.out.println("Fill authorization form with web driver");
        
        WebElement userNameField = driver.findElement(By.ByName.name("username"));
        userNameField.sendKeys("slyisarenko-ilya@mail.ru");
        WebElement passwordField = driver.findElement(By.ByName.name("password"));
        passwordField.sendKeys("LT6h8kXH1234");
        WebElement sendButton = driver.findElement(By.ByCssSelector.cssSelector("input[type='submit']"));
		System.out.println("Submit authorization form with web driver");
        sendButton.submit();
		
		System.out.println("Quit web driver");
		driver.close(); //close selenium window
		driver.quit();
	//		System.out.println(ToStringBuilder.reflectionToString(response, ToStringStyle.MULTI_LINE_STYLE));
		} catch (OAuthSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (server != null && server.isStarted()) {
				try {
				//	server.stop();
				//	System.out.println("Server stopped " );
				} catch (final Exception e) {
					e.printStackTrace();
				}
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

	public static void post(String command, String accessToken) throws ClientProtocolException, IOException {
		HttpPost testRequest = new HttpPost("https://api.hh.ru" + command);
		// testRequest.setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux
		// i686; rv:45.0) Gecko/20100101 Firefox/45.0");
		testRequest.setHeader("User-Agent", "Resume updater/1.0 (slyisarenko-ilya@mail.ru)");
		testRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		// System.out.println(ToStringBuilder.reflectionToString(response.getAllHeaders(),
		// ToStringStyle.MULTI_LINE_STYLE));
		System.out
				.println(ToStringBuilder.reflectionToString(response.getStatusLine(), ToStringStyle.MULTI_LINE_STYLE));

	}

	public static void put(String command, String body, String accessToken)
			throws ClientProtocolException, IOException {

		HttpPut testRequest = new HttpPut("https://api.hh.ru" + command);
		testRequest.setHeader("User-Agent", "Resume updater/1.0 (slyisarenko-ilya@mail.ru)");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);
		testRequest.setHeader("Content-Type", "applicatlion/json");

		HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
		System.out.println(ToStringBuilder.reflectionToString(entity, ToStringStyle.MULTI_LINE_STYLE));

		testRequest.setEntity(entity);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		entity = response.getEntity();
		String entityContents = EntityUtils.toString(entity);
		System.out.println(entityContents);
	}


	
	public static void updateResume() throws Exception {
		retrieveAuthorizationCodeAndUpdateResume();
	}

	public void run() {
		try {
			System.out.println("Try update resume");
			updateResume();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
