package lq.hh.resume.auth.secret;

import lq.hh.resume.auth.ClientIdentity;

public interface SecretSaver {
	public void save(ClientIdentity identity);
}
