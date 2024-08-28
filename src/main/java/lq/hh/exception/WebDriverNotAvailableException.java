package lq.hh.exception;

public class WebDriverNotAvailableException extends RuntimeException {

	public WebDriverNotAvailableException(){
		super();
	}

	public WebDriverNotAvailableException(String message){
		super(message);
	}
}
