package lq.hh.resume.auth.secret.saver;

import lq.hh.resume.auth.entity.ClientIdentity;

public interface SecretSaver {
	public void save(ClientIdentity identity);
}
