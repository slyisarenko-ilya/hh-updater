package lq.hh.exception;

public class WrongPathToChromeExecutable extends RuntimeException {

	public WrongPathToChromeExecutable(){
		super();
	}

	public WrongPathToChromeExecutable(String message, Throwable t){
		super(message, t);
	}
}
