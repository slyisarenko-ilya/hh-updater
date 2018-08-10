package lq.hh.exception;

public class GetSecretException extends RuntimeException {

	private static final long serialVersionUID = 318534375854319362L;

	public GetSecretException(String message) {
		super(message);
	}
	
	public GetSecretException(Throwable e) {
		super(e);
	}

	public GetSecretException(String message, Throwable e) {
		super(message, e);
	}
}
