package controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import models.Friend;
import models.ResponseWrapper;
import models.Squib;
import models.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.*;
import util.*;

@With(AuthenticateRequest.class)
public class SquibController extends Controller {
	
	public static Result postSquib() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null)
			return ok(ResponseWrapper.badRequestNoJson());
		int access = Integer.valueOf(json.findPath("access").textValue());
		float longitude = Float.valueOf(json.findPath("longitude").textValue());
		float latitude = Float.valueOf(json.findPath("latitude").textValue());
		long time_created = System.currentTimeMillis() / 1000l; 
		long time_expired = Long.valueOf(json.findPath("time_expired").textValue()) + time_created;
		String post = json.findPath("squib").textValue();
		
		ArrayList<Long> friends = new ArrayList<Long>();
		if (access == Squib.PRIVATE) {
			JsonNode receivers = json.findPath("receiver_ids");
			for(JsonNode receiver : receivers) {
				Long receiver_id = Long.valueOf(receiver.asText());
				friends.add(receiver_id);
			}
			friends.add(user_id);
		}
		Long post_id = insertSquib(user_id, time_created, time_expired, Squib.VALID, access, longitude, latitude, post);
		try {
			if(access == Squib.PRIVATE)
				insertNewsfeed(post_id, friends);
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Squib did not get saved into newsfeed."));
		}
		return ok(ResponseWrapper.success("post_id", String.valueOf(post_id)));
	}
	
	public static Result editSquib() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null)
			return ok(ResponseWrapper.badRequestNoJson());
		int access = Integer.valueOf(json.findPath("access").textValue());
		float longitude = Float.parseFloat(json.findPath("longitude").textValue());
		float latitude = Float.parseFloat(json.findPath("latitude").textValue());
		long time_created = System.currentTimeMillis() / 1000l; 
		long time_expired = Long.valueOf(json.findPath("time_expired").textValue())  + time_created;
		String post = json.findPath("squib").textValue();
		Long post_id = Long.valueOf(json.findPath("post_id").textValue());
		
		ArrayList<Long> friends = new ArrayList<Long>();
		if (access == Squib.PRIVATE) {
			JsonNode receivers = json.findPath("receiver_ids");
			for(JsonNode receiver : receivers) {
				Long receiver_id = Long.valueOf(receiver.asText());
				friends.add(receiver_id);
			}
			friends.add(user_id);
		}
		try {
			if (checkSquibPrivilege(post_id, user_id) == true) {
				updateSquib(post_id, time_created, time_expired, Squib.VALID, access, longitude, latitude, post);
				if(access == Squib.PRIVATE) 
					updateNewsfeed(user_id, post_id, friends);
			}
			else {
				return ok(ResponseWrapper.unauthorized("Unauthorized to edit squib"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Squib did not get saved."));
		}
		return ok(ResponseWrapper.success("post_id", String.valueOf(post_id)));
	}
	
	public static Result deleteSquib() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		Long post_id = Long.valueOf(json.findPath("post_id").textValue());
		try {
			if (checkSquibPrivilege(post_id, user_id) == true) {
				removeNewsfeed(post_id);
				removeSquib(post_id);
			} else 
				return ok(ResponseWrapper.unauthorized("Unauthorized to delete squib"));
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Squib did not get deleted."));
		}
		return ok(ResponseWrapper.success());
	}
	
	public static Result uploadSquibPic(String post_id) {
		MultipartFormData body = request().body().asMultipartFormData();
		FilePart image = body.getFile("photo");
		String randomName = null;
		File file2 = null;
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		if(image != null) {
			File file = image.getFile();
			randomName = RandomStringGenerator.replacePostPicName();
			file2 = new File(play.Play.application().configuration().getString("squib_media.directory")+File.separator+randomName);
			try {
				FileUtils.moveFile(file, file2);
				Squib squib = Squib.find.byId(Long.valueOf(post_id));
				squib.picture = file2.getName();
			} catch (IOException e) {
				e.printStackTrace();
				return internalServerError(ResponseWrapper.internalServerError(e.getMessage()));
			} finally {
				DatabaseManager.close(null, ps, con);
			}
		} else {
			return ok(ResponseWrapper.notFound("Image not found"));
		}
		return ok(ResponseWrapper.success("filename", file2.getName()));
	}
	
	public static Result getSquibs(String username) {
		User user = User.findByUsername(username);
		invalidateSquibs(user.user_id);
		List<Squib> squibs = Squib.searchSquibs(String.valueOf(user.user_id));
		return ok(ResponseWrapper.success(Json.toJson(squibs)));
	}
	
	public static Result getPrivateSquibFeed() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		Float current_latitude = Float.parseFloat(json.findPath("current_latitude").textValue());
		Float current_longitude = Float.parseFloat(json.findPath("current_longitude").textValue());
		int radius = Integer.valueOf(json.findPath("radius").textValue());
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			invalidateSquibs(user_id);
			ps = con.prepareStatement("select *, truncate((3959 * acos(cos(radians(?)) * cos(radians(latitude)) *"
					+ " cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))), 2)"
					+ " as distance from squib join newsfeed on squib.post_id = newsfeed.post_id"
					+ " where newsfeed.user_id = ? and squib.validation = ? having distance < ? order by time_created desc");
			ps.setFloat(1, current_latitude);
			ps.setFloat(2, current_longitude);
			ps.setFloat(3, current_latitude);
			ps.setLong(4, user_id);
			ps.setInt(5, Squib.VALID);
			ps.setInt(6, radius);
			rs = ps.executeQuery();
			while(rs.next()) {
				ObjectNode result = Json.newObject();
				result.put("post_id", rs.getString("post_id"));
				result.put("time_created", rs.getString("time_created"));
				result.put("time_expired", rs.getString("time_expired"));
				result.put("longitude", rs.getString("longitude"));
				result.put("latitude", rs.getString("latitude"));
				result.put("validation", rs.getString("validation"));
				result.put("access", rs.getString("access"));
				result.put("picture", rs.getString("picture"));
				result.put("squib", rs.getString("post"));
				String squib_user_id = rs.getString("user_id");
				User user = User.findByUserID(squib_user_id);
				result.put("user", Json.toJson(user));
				result.put("friend_status", Friend.findFriendStatus(user_id, user.user_id));
				response.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Cannot retrieve squibs."));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
	}
	
	public static Result getPublicSquibFeed() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		Float current_latitude = Float.parseFloat(json.findPath("current_latitude").textValue());
		Float current_longitude = Float.parseFloat(json.findPath("current_longitude").textValue());
		int radius = Integer.valueOf(json.findPath("radius").textValue());
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("select *, truncate((3959 * acos(cos(radians(?)) * cos(radians(latitude)) *"
					+ " cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))), 2)"
					+ " as distance from squib where access = ? and validation = ? having distance < ?"
					+ " order by time_created desc");
			ps.setFloat(1, current_latitude);
			ps.setFloat(2, current_longitude);
			ps.setFloat(3, current_latitude);
			ps.setString(4, "public");
			ps.setInt(5, Squib.VALID);
			ps.setInt(6, radius);
			rs = ps.executeQuery();
			while(rs.next()) {
				ObjectNode result = Json.newObject();
				result.put("post_id", rs.getString("post_id"));
				result.put("time_created", rs.getString("time_created"));
				result.put("time_expired", rs.getString("time_expired"));
				result.put("longitude", rs.getString("longitude"));
				result.put("latitude", rs.getString("latitude"));
				result.put("validation", rs.getString("validation"));
				result.put("access", rs.getString("access"));
				result.put("picture", rs.getString("picture"));
				result.put("squib", rs.getString("post"));
				String squib_user_id = rs.getString("user_id");
				User user = User.findByUserID(squib_user_id);
				result.put("user", Json.toJson(user));
				result.put("friend_status", Friend.findFriendStatus(user_id, user.user_id));
				response.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Cannot retrieve squibs."));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
	}
	
	private static boolean checkSquibPrivilege(long post_id, long user_id) {
		Squib squib = Squib.find.byId(post_id);
		if(squib.user_id == user_id) 
			return true;
		return false;
	}
	
	private static long insertSquib(long user_id, long time_created, long time_expired, int validation, int access,
			float longitude, float latitude, String post) {
		Squib squib = new Squib();
		squib.user_id = user_id;
		squib.time_created = time_created;
		squib.time_expired = time_expired;
		squib.validation = validation;
		squib.access = access;
		squib.longitude = longitude;
		squib.latitude = latitude; 
		squib.post = post;
		squib.picture = "";
		squib.save();
		long post_id = squib.post_id;
		return post_id;
	}
	
	private static void insertNewsfeed(long post_id, ArrayList<Long> friends) throws SQLException {
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ps = con.prepareStatement("insert into newsfeed(user_id, post_id) values (?, ?)");
		for(long friend: friends) {
			ps.setLong(1, friend);
			ps.setLong(2, post_id);
			ps.addBatch();
		}
		ps.executeBatch();
		DatabaseManager.close(null, ps, con);
	}

	private static void updateSquib(long post_id, long time_created, long time_expired, int validation, int access, 
			float longitude, float latitude, String post) {
		Squib squib = Squib.find.byId(post_id);
		squib.time_created = time_created;
		squib.time_expired = time_expired;
		squib.validation = validation;
		squib.access = access; 
		squib.longitude = longitude;
		squib.latitude = latitude;
		squib.post = post;
		squib.update();
	}
	
	private static void updateNewsfeed(long user_id, long post_id, ArrayList<Long> new_friends) throws SQLException {
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		ArrayList<Long> old_friends = new ArrayList<Long>();
		ArrayList<Long> not_friends = new ArrayList<Long>();
		ps = con.prepareStatement("select user_id from newsfeed where post_id = ?");
		ps.setLong(1, post_id);
		rs = ps.executeQuery();
		while(rs.next()) {
			old_friends.add(rs.getLong("user_id"));
		}
		for(long old_friend : old_friends) {
			if(!(new_friends.contains(old_friend)))
				not_friends.add(old_friend);
		}
		ps.close();
		ps = con.prepareStatement("insert into newsfeed(user_id, post_id) values (?, ?)"
				+ " on duplicate key update post_id = ? and user_id = ?");
		for(long friend : new_friends) {
			ps.setLong(1, friend);
			ps.setLong(2, post_id);
			ps.setLong(3, post_id);
			ps.setLong(4, friend);
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
		if(not_friends != null) {
			ps = con.prepareStatement("delete from newsfeed where post_id = ? and user_id = ?");
			for(Long not_friend : not_friends) {
				ps.setLong(1, post_id);
				ps.setLong(2, not_friend);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		DatabaseManager.close(rs, ps, con);
	}

	private static void removeSquib(long post_id) {
		Squib squib = Squib.find.byId(post_id);
		String squib_picture = squib.picture;
		squib.delete();
		if("".equals(squib_picture) || squib_picture != null) {
			File file = new File(play.Play.application().configuration().getString("squib_media.directory")+File.separator+squib_picture);
			file.delete();
		}
	}
	
	private static void removeNewsfeed(long post_id) throws SQLException { 
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ps = con.prepareStatement("delete from newsfeed where post_id = ?");
		ps.setLong(1, post_id);
		ps.execute();
		DatabaseManager.close(null, ps, con);
	}
	
	private static void invalidateSquibs(Long user_id) {
		Long now = System.currentTimeMillis() / 1000l;
		List<Squib> squibs = Squib.searchSquibs(String.valueOf(user_id));
		for(Squib squib : squibs) {
			if(squib.time_expired < now) {
				squib.validation = Squib.INVALID;
				squib.save();
			}
		}
	}
}
