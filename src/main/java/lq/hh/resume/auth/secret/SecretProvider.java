package lq.hh.resume.auth.secret;

import lq.hh.exception.GetSecretException;
import lq.hh.resume.auth.ClientIdentity;

public interface SecretProvider {
	
	public ClientIdentity get() throws GetSecretException;
}
