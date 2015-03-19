package controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import models.Friend;
import models.ResponseWrapper;
import models.Token;
import models.User;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;
import util.AuthenticateRequest;
import util.DatabaseManager;
import util.ExceptionHandler;
import util.RandomStringGenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.plugin.MailerAPI;
import com.typesafe.plugin.MailerPlugin;

import org.apache.commons.io.FileUtils;
import org.mindrot.jbcrypt.BCrypt;

public class UserController extends Controller {
	
	public static Result createUser() {
		JsonNode json = request().body().asJson();
    	if(json == null) 
    		return ok(ResponseWrapper.badRequestNoJson());
    	User user = new User();
    	Token token = new Token();
    	user.username = json.findPath("username").textValue();
    	user.email = json.findPath("email").textValue();
    	String password = json.findPath("password").textValue();
    	user.password = BCrypt.hashpw(password, BCrypt.gensalt());
    	Connection con = DB.getConnection();
    	PreparedStatement ps = null;
    	try {
    		user.save();
    		token.setUserID(user.user_id);
    		token.save();
    		ps = con.prepareStatement("insert into users_by_email(email, user_id) values (?, ?)");
    		ps.setString(1, user.email);
    		ps.setLong(2, user.user_id);
    		ps.execute();
            
    	} catch(PersistenceException | SQLException e) {
    		token = Token.findByUserID(user.user_id);
    		if(token != null){
    			token.delete();
    		}
    		user = User.find.byId(user.user_id);
    		if(user != null){
    			user.delete();
    		}
    		String message = ExceptionHandler.duplicateCheck(e.getMessage());
    		return ok(ResponseWrapper.badRequest(message));
    	} finally {
    		DatabaseManager.close(null, ps, con);
    	}
        MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
        mail.setSubject("Welcome to Squibbler!");
        mail.setRecipient(user.email);
        mail.setFrom("Phong Nguyen <phonguyen22@gmail.com>");
        mail.send("Keep your friends close... but your enemies closer." );
    	return ok(ResponseWrapper.success("token", token.token));
	}
	
	public static Result loginByUsername() {
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		String username = json.findPath("username").textValue();
		String password = json.findPath("password").textValue(); 
		User user = User.findByUsername(username);
		if(user != null) {
			if(BCrypt.checkpw(password, user.password)) {
				Token token = Token.findByUserID(user.user_id);
				if(token == null) {
					token = new Token(user.user_id);
				} else {
					token.token = Token.generateToken();
				}
				token.save();
				return ok(ResponseWrapper.success("token", token.token));
			}
		}
		return ok(ResponseWrapper.unauthorized("Incorrect Login"));
	}
	
	public static Result loginByEmail() {
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		String email = json.findPath("email").textValue();
		String password = json.findPath("password").textValue();
		User user = User.findByEmail(email);
		if(user != null) {
			if(BCrypt.checkpw(password, user.password)) {
				Token token = Token.findByUserID(user.user_id);
				if(token == null) {
					token = new Token(user.user_id);
				} else {
					token.token = Token.generateToken();
				}
				token.save();
				return ok(ResponseWrapper.success("token", token.token));
			}
		}
		return ok(ResponseWrapper.unauthorized("Incorrect Login"));
	}
	
	@With(AuthenticateRequest.class)
	public static Result changePassword() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		String currentPassword = json.findPath("current_password").textValue();
		String newPassword = json.findPath("new_password").textValue(); 
		User user = User.find.byId(user_id);
		if(BCrypt.checkpw(currentPassword, user.password)) {
			String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
			user.password = newHashedPassword;
			user.save();
			return ok(ResponseWrapper.success());
		} else
			return ok(ResponseWrapper.unauthorized("Incorrect Password"));
	}
	
	@With(AuthenticateRequest.class)
    public static Result updateUser() {
		Long user_id = (Long) Context.current().args.get("user_id");
    	JsonNode json = request().body().asJson();
    	if(json == null)
    		return ok(ResponseWrapper.badRequestNoJson());
    	User user = User.find.byId(user_id);
    	user.email = json.findPath("email").textValue();
    	user.phone_number = json.findPath("phone_number").textValue();
    	user.name = json.findPath("name").textValue();
    	user.gender = Integer.valueOf(json.findPath("gender").textValue());
    	user.fb_id = json.findPath("fb_id").textValue();
    	user.bio = json.findPath("bio").textValue();
    	Connection con = DB.getConnection();
    	PreparedStatement ps = null;
    	try {
    		user.update();
			if(user.fb_id != null && !("".equals(user.fb_id))) {
				ps = con.prepareStatement("insert into users_by_fb(fb_id, user_id) values (?, ?) "
						+ "on duplicate key update fb_id = ?");
				ps.setString(1, user.fb_id);
				ps.setLong(2, user.user_id);
				ps.setString(3, user.fb_id);
				ps.execute();
				ps.close();
			}
			if(user.phone_number != null && !("".equals(user.phone_number))) {
				ps = con.prepareStatement("insert into users_by_phone(phone_number, user_id) values (?, ?) "
						+ "on duplicate key update phone_number = ?");
				ps.setString(1, user.phone_number);
				ps.setLong(2, user.user_id);
				ps.setString(3, user.phone_number);
				ps.execute();
				ps.close();
			}
			ps = con.prepareStatement("update users_by_email set email = ? where user_id = ?");
			ps.setString(1, user.email);
			ps.setLong(2, user.user_id);
			ps.execute();
	    	return ok(ResponseWrapper.success());
    	} catch (PersistenceException | SQLException e) {
    		String message = ExceptionHandler.duplicateCheck(e.getMessage());
    		return ok(ResponseWrapper.badRequest(message));
    	} finally {
    		DatabaseManager.close(null, ps, con);
    	}
    }

	@With(AuthenticateRequest.class)
	public static Result getUserDetails(String username) {
		Long user_id = (Long) Context.current().args.get("user_id");
		User user = User.findByUsername(username);
		if(user == null)
			return ok(ResponseWrapper.notFound("User does not exists."));
		ObjectNode result = Json.newObject();
		result.put("user", Json.toJson(user));
		result.put("friend_status", Friend.findFriendStatus(user_id, user.user_id));
		return ok(ResponseWrapper.success(result));
	}
	
	@With(AuthenticateRequest.class)
	public static Result uploadProfilePic() {
		Long user_id = (Long) Context.current().args.get("user_id");
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart image = body.getFile("photo");
		String randomName = null;
		File file2 = null;
		if(image != null) {
			File file = image.getFile();
			randomName = RandomStringGenerator.replaceProfPicName();
			file2 = new File(play.Play.application().configuration().getString("user_media.directory")+File.separator+randomName);
			try {
				FileUtils.moveFile(file, file2);
				User user = User.find.byId(user_id);
				if(!("".equals(user.profile_picture)) || user.profile_picture != null) {
					deleteProfilePic(user.profile_picture);
				}
				user.profile_picture = randomName;
				user.update();
			} catch (IOException e) {
				e.printStackTrace();
				return ok(ResponseWrapper.internalServerError(e.getMessage()));
			}
		} else {
			return ok(ResponseWrapper.notFound("Image not found"));
		}
		return ok(ResponseWrapper.success("filename", file2.getName()));
	}
	
	private static void deleteProfilePic(String pic) throws IOException {
		File file = new File(play.Play.application().configuration().getString("user_media.directory")+File.separator+pic);
		file.delete();
	}

	@With(AuthenticateRequest.class)
	public static Result removeProfilePic() {
		Long user_id = (Long) Context.current().args.get("user_id");
		User user = User.find.byId(user_id);
		String picture_name = user.profile_picture;
		try {
			deleteProfilePic(picture_name);
			user.profile_picture = "";
			user.save();
		} catch (IOException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.internalServerError(e.getMessage()));
		}
		return ok(ResponseWrapper.success());
	}
	
	@With(AuthenticateRequest.class)
	public static Result searchByUsernameOrName() {
		Long user_id = (Long) Context.current().args.get("user_id");
		String search_field = "%" + request().getQueryString("q") + "%";
		List<User> users = User.searchForUser(search_field);
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		for(User user : users) {
			ObjectNode result = Json.newObject();
			result.put("user", Json.toJson(user));
			result.put("friend_status", Friend.findFriendStatus(user_id, user.user_id));
			response.add(result);
		}
		return ok(ResponseWrapper.success(response));
	}
	
	@With(AuthenticateRequest.class) 
	public static Result logout() {
		Long user_id = (Long) Context.current().args.get("user_id");
		Token token = Token.findByUserID(user_id);
		token.delete();
		return ok(ResponseWrapper.success());
	}
	
	@With(AuthenticateRequest.class)
	public static Result findFriendsByFacebook() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		JsonNode jsonFb = json.get("fb_ids");
		if(jsonFb == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		ArrayList<String> fb_ids = new ArrayList<String>();
		for(JsonNode fbString : jsonFb) {
			String fb_id = fbString.asText();
			fb_ids.add(fb_id);
		}
		final StringBuilder updateQuery = new StringBuilder("select * from user join users_by_fb "
				+ "on user.user_id = users_by_fb.user_id where users_by_fb.fb_id in(");
		for(int i = 0; i < fb_ids.size(); i++) {
			updateQuery.append("?,");
		}
		updateQuery.deleteCharAt(updateQuery.length() - 1);
		updateQuery.append(")");
		String fb = updateQuery.toString();
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(fb);
			int i = 1;
			for(String fb_id : fb_ids) 
				ps.setString(i++, fb_id);
			rs = ps.executeQuery();
			while(rs.next()) {
				ObjectNode result = Json.newObject();
				Long friend_id = rs.getLong("user_id");
				result.put("user_id", friend_id);
				result.put("username", rs.getString("username"));
				result.put("name", rs.getString("name"));
				result.put("bio", rs.getString("bio"));
				result.put("profile_picture", rs.getString("profile_picture"));
				result.put("friends_count", rs.getInt("friends_count"));
				result.put("friend_status", Friend.findFriendStatus(user_id, friend_id));
				response.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return internalServerError(ResponseWrapper.internalServerError(e.getMessage()));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
	}
	
	@With(AuthenticateRequest.class)
	public static Result findFriendsByPhoneNumbers() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		JsonNode jsonPn = json.get("phone_numbers");
		if(jsonPn == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		ArrayList<String> phone_numbers = new ArrayList<String>(); 
		for(JsonNode pnString : jsonPn) {
			String phone_number = pnString.asText();
			phone_numbers.add(phone_number);
		}
		final StringBuilder updateQuery = new StringBuilder("select * from user join users_by_phone "
				+ "on user.user_id = users_by_phone.user_id where users_by_phone.phone_number in(");
		for(int i = 0; i < phone_numbers.size(); i++) {
			updateQuery.append("?,");
		}
		updateQuery.deleteCharAt(updateQuery.length() - 1);
		updateQuery.append(")");
		String pn = updateQuery.toString();
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(pn);
			int i = 1;
			for(String phone_number : phone_numbers) 
				ps.setString(i++, phone_number);
			rs = ps.executeQuery();
			while(rs.next()) {
				ObjectNode result = Json.newObject();
				Long friend_id = rs.getLong("user_id");
				result.put("user_id", friend_id);
				result.put("username", rs.getString("username"));
				result.put("name", rs.getString("name"));
				result.put("bio", rs.getString("bio"));
				result.put("profile_picture", rs.getString("profile_picture"));
				result.put("friends_count", rs.getInt("friends_count"));
				result.put("friend_status", Friend.findFriendStatus(user_id, friend_id));
				response.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return internalServerError(ResponseWrapper.internalServerError(e.getMessage()));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
	}
	
	public static Result checkFacebookID(String fb_id) {
		int exists = User.checkIfExists("fb_id", fb_id);
		return ok(ResponseWrapper.success("exists", String.valueOf(exists)));
	}
	
	public static Result checkPhoneNumber(String phone_number) {
		int exists = User.checkIfExists("phone_number", phone_number);
		return ok(ResponseWrapper.success("exists", String.valueOf(exists)));
	}
	
	public static Result deleteUser() {
		Long user_id = (Long) Context.current().args.get("user_id");
		User user = User.find.byId(user_id);
		user.delete();
		return ok(ResponseWrapper.success());
	}
}