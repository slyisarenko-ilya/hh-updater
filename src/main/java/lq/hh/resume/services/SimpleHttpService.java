package lq.hh.resume.services;

import java.io.IOException;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHttpService implements HttpService {

	private static final Logger logger = LoggerFactory.getLogger(SimpleHttpService.class);

	public String get(String command, String accessToken) throws IOException {
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

	public void put(String command, String body, String accessToken)
			throws IOException {

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

	public HttpResponse post(String command, String accessToken) throws IOException {
		HttpPost testRequest = new HttpPost("https://api.hh.ru" + command);
		// testRequest.setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux
		// i686; rv:45.0) Gecko/20100101 Firefox/45.0");
		testRequest.setHeader("User-Agent", "Resume updater/1.0");
		testRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		testRequest.setHeader("Authorization", "Bearer " + accessToken);

		HttpClient httpClient = HttpClients.createDefault();
		HttpResponse response = httpClient.execute(testRequest);

		// "About statuses you can read here: https://tech.hh.ru/api/rendered-docs/docs/resumes.md.html#publish"
		if(logger.isDebugEnabled()) {
			String rawServerResponse = ToStringBuilder.reflectionToString(response.getStatusLine(), ToStringStyle.MULTI_LINE_STYLE);
			logger.debug("Ответ сервера в сыром виде: {}", rawServerResponse);
		}

		return response;
	}

}
