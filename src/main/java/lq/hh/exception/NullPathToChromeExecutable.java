package lq.hh.exception;

public class NullPathToChromeExecutable extends RuntimeException {

	public NullPathToChromeExecutable(){
		super();
	}

	public NullPathToChromeExecutable(String message, Throwable t){
		super(message, t);
	}
}
