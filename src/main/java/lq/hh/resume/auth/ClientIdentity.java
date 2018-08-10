package lq.hh.resume.auth;

public class ClientIdentity {

	
	private String clientSecret;
	private String clientId;
	private String hhUserName;
	private String hhPassword;
	
	public ClientIdentity() {
		
	}
	
	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getHhUserName() {
		return hhUserName;
	}

	public void setHhUserName(String hhUserName) {
		this.hhUserName = hhUserName;
	}

	public String getHhPassword() {
		return hhPassword;
	}

	public void setHhPassword(String hhPassword) {
		this.hhPassword = hhPassword;
	}

	@Override
	public String toString() {
		return "ClientIdentity [clientSecret=" + clientSecret + ", clientId=" + clientId + ", hhUserName=" + hhUserName
				+ ", hhPassword=" + hhPassword + "]";
	}

	
	
}
