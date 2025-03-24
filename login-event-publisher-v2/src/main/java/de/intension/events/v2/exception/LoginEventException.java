package de.intension.events.v2.exception;

/**
 * 
 * @author kdeshpande
 *
 */
public class LoginEventException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LoginEventException(Throwable cause) {
		super(cause);
	}

	public LoginEventException(String message) {
		super(message);
	}

	public LoginEventException(String message, Throwable cause) {
		super(message, cause);
	}

}
