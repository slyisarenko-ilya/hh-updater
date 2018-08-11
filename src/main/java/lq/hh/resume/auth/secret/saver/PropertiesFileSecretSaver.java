package lq.hh.resume.auth.secret.saver;

import lq.hh.resume.auth.entity.ClientIdentity;
import lq.hh.resume.services.PropertiesService;

public class PropertiesFileSecretSaver implements SecretSaver {

	public void save(ClientIdentity identity) {
		PropertiesService.storeProperty(PropertiesService.CLIENT_SECRET, identity.getClientSecret());
		PropertiesService.storeProperty(PropertiesService.CLIENT_ID, identity.getClientId());
		PropertiesService.storeProperty(PropertiesService.HEADHUNT_USERNAME, identity.getHhUserName());
		PropertiesService.storeProperty(PropertiesService.HEADHUNT_PASSWORD, identity.getHhPassword());
	}
}
