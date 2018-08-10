package lq.hh.resume.auth.token;

public class Token {
	private String value;
	
	public Token(String value) {
		this.value = value;
	}
	
	public String get() {
		return value;
	}
	
	public void set(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
