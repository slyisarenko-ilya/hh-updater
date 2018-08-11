package lq.hh.resume.auth.secret.provider;

import lq.hh.exception.GetSecretException;
import lq.hh.resume.auth.entity.ClientIdentity;

public interface SecretProvider {
	
	public ClientIdentity get() throws GetSecretException;
}
