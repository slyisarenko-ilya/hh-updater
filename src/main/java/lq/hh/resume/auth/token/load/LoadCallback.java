package lq.hh.resume.auth.token.load;

public interface LoadCallback {
	void run(String code) throws Exception;
}
