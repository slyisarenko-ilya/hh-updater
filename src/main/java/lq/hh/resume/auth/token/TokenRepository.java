package lq.hh.resume.auth.token;

public interface TokenRepository {
	String load();
	
	void save(String token);
}
