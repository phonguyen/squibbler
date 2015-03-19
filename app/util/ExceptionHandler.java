package util;

public class ExceptionHandler {
	
	private static final String username = "username";
	private static final String email = "email";
	private static final String user_id = "user_id";
	private static final String duplicate = "duplicate";

	public static String duplicateCheck(String message) {
		if(message.toLowerCase().contains(username) && message.toLowerCase().contains(duplicate))
			return "Username is already taken.";
		if(message.toLowerCase().contains(email) && message.toLowerCase().contains(duplicate))
			return "Email is already taken.";
		if(message.toLowerCase().contains(user_id) && message.toLowerCase().contains(duplicate))
			return "ID error - please retry.";
		return message;
	}
}
