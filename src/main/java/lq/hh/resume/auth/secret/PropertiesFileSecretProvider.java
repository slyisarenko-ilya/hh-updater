package lq.hh.resume.auth.secret;

import lq.hh.exception.GetSecretException;
import lq.hh.exception.NoPropertiesException;
import lq.hh.resume.auth.ClientIdentity;
import lq.hh.resume.services.PropertiesService;

public class PropertiesFileSecretProvider implements SecretProvider {


	public ClientIdentity get() throws GetSecretException {
		
		try {
			ClientIdentity identity = new ClientIdentity();
			identity.setClientSecret(PropertiesService.loadProperty(PropertiesService.CLIENT_SECRET));
			identity.setClientId(PropertiesService.loadProperty(PropertiesService.CLIENT_ID));
			identity.setHhUserName(PropertiesService.loadProperty(PropertiesService.HEADHUNT_USERNAME));
			identity.setHhPassword(PropertiesService.loadProperty(PropertiesService.HEADHUNT_PASSWORD));
			return identity;
		} catch (NoPropertiesException e) {
			throw new GetSecretException("Can't load secrets from properties file");
		}
	}

}
