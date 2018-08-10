package lq.hh.exception;

public class CannotUpdateException extends RuntimeException {

	private static final long serialVersionUID = 7712193792875366608L;

	public CannotUpdateException(String s, Throwable e){
		super(s, e);
	}

	public CannotUpdateException(String s){
		super(s);
	}

	public CannotUpdateException(Throwable t){
		super(t);
	}
}
