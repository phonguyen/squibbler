package models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.data.validation.Constraints;
import play.data.format.Formats;
import play.db.ebean.Model;

@Entity
public class User extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public long user_id;
	
	@Constraints.Required
	@Formats.NonEmpty
	@Column(unique = true) 
	public String username; 
	
	@Constraints.Required
	@Formats.NonEmpty
	@JsonIgnore
	public String password;

	@Constraints.Required
	@Formats.NonEmpty
	@Column(unique = true)
	public String email;
	
	public String phone_number = "";
	public String name = "";	
	public int gender = 0;
	public String fb_id = "";
	public String bio = "";
	public int friends_count = 0;
	public String profile_picture = "";
	
	public static Model.Finder<Long, User> find = new Model.Finder<Long, User>(Long.class, User.class);
	
	public static User findByUserID(String user_id) {
		return find.where().eq("user_id", user_id).findUnique();
	}
	
	public static User findByEmail(String email) {
		return find.where().eq("email", email).findUnique();
	}
	
	public static User findByUsername(String username) {
		return find.where().eq("username", username).findUnique();
	}
	
	public static User findByFbID(String fb_id) {
		return find.where().eq("fb_id", fb_id).findUnique();
	}
	
	public static User findByPhoneNumber(String phone_number) {
		return find.where().eq("phone_number", phone_number).findUnique();
	}
	
	/*public static String findByProfilePic(String profile_pic) {
		User user = find.where().eq("profile_pic", profile_pic).findUnique();
		return user.profile_pic;
	}*/
	
	public static int checkIfExists(String key, String value) {
		return find.where().eq(key, value).findRowCount();
	}
	
	public static List<User> searchForUser(String search_field) {
		List<User> username = find.where().ilike("username", search_field).findList();
		List<User> name = find.where().ilike("name", search_field).findList();
		username.addAll(name);
		return username;
	}
}