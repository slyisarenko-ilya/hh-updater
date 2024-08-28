package lq.hh.resume;

import lq.hh.exception.CannotUpdateException;
import lq.hh.resume.auth.entity.ClientIdentity;
import lq.hh.resume.auth.secret.SecretManager;
import lq.hh.resume.auth.token.load.SeleniumTokenLoader;
import lq.hh.resume.auth.token.load.TokenLoader;
import lq.hh.resume.auth.token.repo.PropertiesFileTokenRepository;
import lq.hh.resume.auth.token.repo.TokenRepository;
import lq.hh.resume.services.HttpService;
import lq.hh.resume.services.InfoService;
import lq.hh.resume.services.SimpleHttpService;
import lq.hh.resume.services.VariablesService;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
public class ResumeUpdater{

    private static final Logger logger = LoggerFactory.getLogger(ResumeUpdater.class);

	private SecretManager secretManager;
	private TokenRepository tokenRepository;
	private TokenLoader tokenLoader;
	private static final String NUMBER_OF_FETCH_TOKEN_ATTEMPTS = "NUMBER_OF_FETCH_TOKEN_ATTEMPTS";
	private static final Integer DEFAULT_FETCH_TOKEN_ATTEMPTS_COUNT = 5;
	private ClientIdentity identity;
	private VariablesService variablesService;
	private static final String CHROME_BINARY = "CHROME_BINARY";
	private HttpService httpService;

	private InfoService infoService;

	public ResumeUpdater() {
		 secretManager = new SecretManager();
		 tokenRepository = new PropertiesFileTokenRepository();
		 identity = secretManager.getClientIdentity();
		 secretManager.storeClientIdentity(identity);
		 httpService = new SimpleHttpService();
		 infoService = new InfoService();
		 variablesService = new VariablesService();
		 String chromeBinary = variablesService.getString(CHROME_BINARY);
		 Integer fetchTokenAttempts = variablesService.getInt(NUMBER_OF_FETCH_TOKEN_ATTEMPTS, DEFAULT_FETCH_TOKEN_ATTEMPTS_COUNT);
		 tokenLoader = new SeleniumTokenLoader(identity, fetchTokenAttempts, chromeBinary, variablesService);
	}
	
	public void start() {
		
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
			} catch (CannotUpdateException e) {
				logger.error("", e);
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
			String jsonResumes = httpService.get("/resumes/mine", accessToken);

			JSONObject resumes = new JSONObject(jsonResumes);
			JSONArray items = resumes.getJSONArray("items");
			if (items.length() > 0) {
				JSONObject firstResume = items.getJSONObject(0);
				String resumeId = firstResume.getString("id");
				logger.debug("Try update resume: " + resumeId);

				// update resume
				HttpResponse httpResponse = httpService.post("/resumes/" + resumeId + "/publish", accessToken);
				Integer responseCode = infoService.getStatusCode(httpResponse);
				String message = infoService.getMessageByStatusCode(responseCode);
				logger.info(message);
			}
		} catch (Exception e) {
			throw new CannotUpdateException("Exception when handle resume information", e);
		}
	}
 
	
	public static void main(String[] args) {
		ResumeUpdater app = new ResumeUpdater();
		app.start();
	}

}
