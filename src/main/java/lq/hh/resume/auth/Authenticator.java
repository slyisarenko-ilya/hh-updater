package lq.hh.resume.auth;

import lq.hh.resume.auth.secret.SecretProvider;

public class Authenticator {
	
	private SecretProvider secretProvider;
	
	public Authenticator(SecretProvider secretProvider) {
		this.secretProvider = secretProvider;
	}
	
	public void authenticate() {
		
	}
	
	public void getAccessToken() {
		
	}
	
}
