package lq.hh.resume.auth.secret;

import java.util.ArrayList;
import java.util.List;

import lq.hh.exception.GetSecretException;
import lq.hh.resume.auth.ClientIdentity;

public class SecretManager {
	List<SecretProvider> secretProviders;
	
	SecretSaver secretSaver;
	
	public SecretManager() {
		secretProviders = new ArrayList<SecretProvider>();
		secretProviders.add(new PropertiesFileSecretProvider());
		secretProviders.add(new ConsoleSecretProvider());
		secretSaver = new PropertiesFileSecretSaver();
	}
	
	public ClientIdentity getClientIdentity() {
		ClientIdentity identity = null;
		for(SecretProvider provider: secretProviders) {
			try {
				identity = provider.get();
			} catch(GetSecretException e) {
				identity = null;
			}
			if(identity != null) {
				return identity;
			}
		}
		throw new GetSecretException("Can't get identity");
	}
	
	public void storeClientIdentity(ClientIdentity identity) {
		secretSaver.save(identity);
	}
	
}
