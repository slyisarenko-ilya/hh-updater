package lq.hh.exception;

public class NoPropertiesException extends RuntimeException {

	private static final long serialVersionUID = -6406419135192637832L;
	
	public NoPropertiesException(String message) {
		super(message);
	}

	public NoPropertiesException(Throwable e) {
		super(e);
	}
}
