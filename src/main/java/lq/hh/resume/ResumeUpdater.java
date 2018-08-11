package lq.hh.resume;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lq.hh.exception.CannotUpdateException;
import lq.hh.resume.auth.entity.ClientIdentity;
import lq.hh.resume.auth.secret.SecretManager;
import lq.hh.resume.auth.token.load.SeleniumTokenLoader;
import lq.hh.resume.auth.token.load.TokenLoader;
import lq.hh.resume.auth.token.repo.PropertiesFileTokenRepository;
import lq.hh.resume.auth.token.repo.TokenRepository;
import lq.hh.resume.services.HttpService;
//
public class ResumeUpdater{

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
			} catch (Exception e1) {
				logger.error("App exception", e1);
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
		ResumeUpdater app = new ResumeUpdater();
		app.start();
	}
	
	private static void testSystemPropertiesAvailability() {
        logger.debug(System.getProperty("webdriver.chrome.driver"));
        assert System.getProperty("webdriver.chrome.driver") != null;
	}
}
