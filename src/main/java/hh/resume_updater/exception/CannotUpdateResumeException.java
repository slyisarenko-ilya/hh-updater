package hh.resume_updater.exception;

public class CannotUpdateResumeException extends Exception {

	public CannotUpdateResumeException(String s, Throwable e){
		super(s, e);
	}

	public CannotUpdateResumeException(String s){
		super(s);
	}

}
