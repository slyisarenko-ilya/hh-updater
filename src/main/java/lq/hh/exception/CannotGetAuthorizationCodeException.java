package lq.hh.exception;

public class CannotGetAuthorizationCodeException extends RuntimeException {

	public CannotGetAuthorizationCodeException(String s, Throwable e){
		super(s, e);
	}

	public CannotGetAuthorizationCodeException(String s){
		super(s);
	}

	public CannotGetAuthorizationCodeException(Throwable t){
		super(t);
	}
}
