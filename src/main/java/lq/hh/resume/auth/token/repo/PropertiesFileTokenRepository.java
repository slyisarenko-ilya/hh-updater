package lq.hh.resume.auth.token.repo;

import lq.hh.resume.services.PropertiesService;

public class PropertiesFileTokenRepository implements TokenRepository{
	private static final String TOKEN = "token";

	@Override
	public String load() {
		return PropertiesService.loadProperty(TOKEN);
	}
	
	@Override
	public void save(String token) {
		PropertiesService.storeProperty(TOKEN, token);
	}
}
