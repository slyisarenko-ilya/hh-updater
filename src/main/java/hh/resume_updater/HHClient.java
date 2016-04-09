package hh.resume_updater;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.as.request.OAuthRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.httpclient4.HttpClient4;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class HHClient {
	private static final String CLIENT_ID = "G7RLL36FL0Q69S2B8UA6SRE4E16886P551C1O82I1B9VOR27P0ET3IULCAN24SBU";
	private static final String CLIENT_SECRET = "ORKJLA8JPF7T23D4FJALC7F4E7HU8S901H7MB07RNPE44V78OS4T42S40TMMI3DV"; 

	public static void handleCodeFetched(String code) throws OAuthProblemException{
			System.out.println("Fetch access_token by authorization_code (" + code + ")");

		/*
			 * Приложение делает сервер-сервер POST-запрос на
			 * https://m.hh.ru/oauth/token для обмена полученного
			 * authorization_code на access_token. В запросе необходимо
			 * передать:
			 * grant_type=authorization_code&client_id={CLIENT_ID}&client_secret
			 * ={CLIENT_SECRET}&code={CODE} Тело запроса необходимо передавать в
			 * стандартном application/x-www-form-urlencoded с указанием
			 * соответствующего заголовка Content-Type.
			 * 
			 */

			System.out.println("\n[STEP] Request Access Token");
			OAuthClientRequest request = null;
			try {
							
				request = OAuthClientRequest.tokenLocation("https://hh.ru/oauth/token/")
				 	.setGrantType(GrantType.AUTHORIZATION_CODE)
         			.setClientId(CLIENT_ID)
					.setClientSecret(CLIENT_SECRET)
					.setCode(code)
					.buildBodyMessage();
//				request.setHeader("Content-Type", "application/x-www-form-urlencoded");
									
			 Map<String, String> headers = new HashMap<String, String>();
	        headers.put("Content-Type", "application/x-www-form-urlencoded");
	
	        // create OAuth client that uses custom http client under the hood
	        URLConnectionClient urlConnectionClient = new URLConnectionClient();
	        OAuthJSONAccessTokenResponse jsonResponse = urlConnectionClient.execute(request, headers,
	                "POST", OAuthJSONAccessTokenResponse.class);
	                
			System.out.println(jsonResponse.getBody());
			
						//.buildBodyMessage()
			
						;
			} catch (OAuthSystemException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}

		
	}
	

	/**
	 * @param args
	 * @throws Exception 
	 */

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		final Server server = new Server(8080);
		try {
			/*
			 * Если пользователь не разрешает доступ приложению, мы
			 * перенаправляем пользователя на указанный redirect_uri с
			 * ?error=access_denied и state={STATE}, если таковой был указан при
			 * первом запросе. Иначе в редиректе мы указываем временный
			 * authorization_code:
			 */
			String code = "no code";

			Handler h = new AbstractHandler(){

			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
			        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					if(target.contains("/code")){		
						String code = baseRequest.getParameter("code");
						if(code != null && !code.isEmpty()){
							try {
								HHClient.handleCodeFetched(code);
							} catch (OAuthProblemException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					        response.setStatus(HttpServletResponse.SC_OK);
					        baseRequest.setHandled(true);
						}
//							try {
//								server.stop();
//								System.out.println("Jetty server stopped");
//							} catch (Exception e) {
//								// TODO Auto-generated catch block
//								//e.printStackTrace();
//							}			
					}
			        response.setContentType("text/html;charset=utf-8");
				}
			};
	 
	        server.setHandler( h );
		
	        server.start();
//	        server.join();
			
			/*
			 * Приложение направляет пользователя по адресу:
			 * https://m.hh.ru/oauth/authorize?response_type=code&client_id={
			 * CLIENT_ID}&state={STATE}. Параметр state опционален, в случае его
			 * указания, он будет включен в ответный редирект — После
			 * прохождения авторизации на сайте, мы запрашиваем у пользователя
			 * разрешение доступа приложения к его персональным данным.
			 */
			
			OAuthClientRequest request = OAuthClientRequest
					.authorizationLocation("https://hh.ru/oauth/authorize")
					.setClientId(CLIENT_ID)
					.setResponseType("code")
					//.setRedirectURI("http://localhost/code")
					.buildQueryMessage();
	        	
	        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        	request.addHeader("Content-Type", "text/html");
        	request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        	
        	request.addHeader("Accept-Language", "en-US,en;q=0.5");
        	request.addHeader("Connection", "keep-alive");
          	request.addHeader("Cookie", "hhtoken=O0yhnWdtanoutAdVwrhZao4ynKe0; hhuid=gRzscqtau6_FElbysoIx!Q--;");
        	request.addHeader("Host", "hh.ru");
        	request.addHeader("Proxy-Authorization", "a3ea6d4a65eee8d50e7e6bffb2476845d6d0e214b049f571e3e38a25c4dffa9bc54fcd7d44fdb4c6");
        	request.addHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:45.0) Gecko/20100101 Firefox/45.0");
        	request.addHeader("X-Compress", "1");
        	
//	        System.out.println(ToStringBuilder.reflectionToString(request, ToStringStyle.MULTI_LINE_STYLE));
        	
            OAuthClient oAuthClient = new OAuthClient(new HttpClient4());
            
			oAuthClient.resource(request, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
	        
	        //System.out.println(ToStringBuilder.reflectionToString(response, ToStringStyle.MULTI_LINE_STYLE));
	
			
		} catch (OAuthSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(server != null && server.isStarted()){
				server.stop();
			} 
		}

	}
}