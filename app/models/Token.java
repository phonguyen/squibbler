package models;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.data.format.Formats;

@Entity
public class Token extends Model {

	private static final long serialVersionUID = 1L;

	@Constraints.Required
	@Formats.NonEmpty
	public String token;
	
	@Id
	public Long user_id;
	
	@Constraints.Required
	@Formats.NonEmpty
	public String exp_date;
	
	private static SecureRandom random;
	private static final long thirtyDays = 2592000;
	
	public static Model.Finder<String, Token> find = new Finder<String, Token>(String.class, Token.class);
	
	public Token() {
		init();
		long exp_date = (System.currentTimeMillis() / 1000l) + thirtyDays;
		this.token = generateToken();
		this.exp_date = String.valueOf(exp_date);
	}
	
	public Token(Long user_id) {
		init();
		long exp_date = (System.currentTimeMillis() / 1000l) + thirtyDays;
		this.token = generateToken();
		this.user_id = user_id;
		this.exp_date = String.valueOf(exp_date);
	}
	
	public static Token findByToken(String token) {
		return find.where().eq("token", token).findUnique();
	}
	
	public static Token findByUserID(Long user_id) {
		return find.where().eq("user_id", user_id).findUnique();
	}
	
	public static void init() {
		if (random == null)
			random = new SecureRandom();
	}
	
	public static String generateToken() {
		return new BigInteger(130, random).toString(32);
	}
	
	public String getToken() {
		return this.token;
	}
	
	public void setUserID(long user_id) {
		this.user_id = user_id;
	}
}
