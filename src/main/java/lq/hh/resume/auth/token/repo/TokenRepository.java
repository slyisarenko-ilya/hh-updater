package lq.hh.resume.auth.token.repo;

public interface TokenRepository {
	String load();
	
	void save(String token);
}
