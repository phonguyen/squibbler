package controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.Friend;
import models.Notification;
import models.ResponseWrapper;
import models.User;
import play.db.DB;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Context;
import util.AuthenticateRequest;
import util.DatabaseManager;

@With(AuthenticateRequest.class)
public class FriendController extends Controller {

	public static Result addFriendRequest(String friend) {
		Long user_id = (Long) Context.current().args.get("user_id");
		User friend_details = User.findByUsername(friend);
		if(friend == null)
			return ok(ResponseWrapper.notFound("User not found."));
		Friend friend_request = new Friend();
		friend_request.user = user_id;
		friend_request.friend = friend_details.user_id;
		friend_request.status = Friend.PENDING;
		friend_request.save();
		
		Friend friend_requested = new Friend();
		friend_requested.user = friend_details.user_id;
		friend_requested.friend = user_id;
		friend_requested.status = Friend.CONFIRM;
		friend_requested.save();
		NotificationController.insertNotification(friend_request.friend, Notification.FRIEND_REQUEST, Notification.NOT_SEEN);
		return ok(ResponseWrapper.success());
	}
	
	public static Result addFriendRequests() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		JsonNode json_friend_ids = json.get("friend_ids");
		if(json_friend_ids == null)
			return ok(ResponseWrapper.badRequestNoJson());
		ArrayList<Long> friend_ids = new ArrayList<Long>();
		for(JsonNode json_friend_id : json_friend_ids) {
			Long friend_id = Long.valueOf(json_friend_id.asText());
			friend_ids.add(friend_id);
		}
		for(Long friend_id : friend_ids) {
			Friend friend_request = new Friend();
			friend_request.user = user_id;
			friend_request.friend = friend_id;
			friend_request.status = Friend.PENDING;
			friend_request.save();
			
			Friend friend_requested = new Friend();
			friend_requested.user = friend_id;
			friend_requested.friend = user_id;
			friend_requested.status = Friend.CONFIRM;
			friend_request.save();
		}
		NotificationController.insertNotification(friend_ids, Notification.FRIEND_REQUEST, Notification.NOT_SEEN);
		return ok(ResponseWrapper.success());
	}

	public static Result cancelFriendRequest(String friend) {
		Long user_id = (Long) Context.current().args.get("user_id");
		User friend_details = User.findByUsername(friend);
		if(friend_details == null)
			return ok(ResponseWrapper.notFound("User not found."));
		Friend friend_request = Friend.find(user_id, friend_details.user_id, Friend.PENDING);
		friend_request.delete();
		Friend friend_requested = Friend.find(friend_details.user_id, user_id, Friend.CONFIRM);
		friend_requested.delete();
		NotificationController.deleteNotification(friend_details.user_id, user_id, Notification.FRIEND_REQUEST);
		return ok(ResponseWrapper.success());
	}

	public static Result respondFriendRequest() {
		Long user_id = (Long) Context.current().args.get("user_id");
		JsonNode json = request().body().asJson();
		if(json == null) 
			return ok(ResponseWrapper.badRequestNoJson());
		Long friend_id = Long.valueOf(json.findPath("user_id").textValue());
		String response = json.findPath("response").textValue();
		if(response.equals("accept"))
			return acceptFriendRequest(user_id, friend_id);
		else if(response.equals("deny"))
			return denyFriendRequest(user_id, friend_id);
		else
			return ok(ResponseWrapper.badRequest("Invalid response."));
	}
	
	public static Result acceptFriendRequest(Long user_id, Long friend_id) {
		Friend friend_request = Friend.find(friend_id, user_id, Friend.PENDING);
		friend_request.status = Friend.ACCEPTED;
		friend_request.save();
		
		Friend friend_requested = Friend.find(user_id, friend_id, Friend.CONFIRM);
		friend_requested.status = Friend.ACCEPTED;
		friend_requested.save();
		
		User user = User.find.byId(user_id);
		user.friends_count++;
		user.save();
		User friend = User.find.byId(friend_id);
		friend.friends_count++;
		friend.save();
		
		NotificationController.insertNotification(friend_id, Notification.ACCEPTED_FRIEND_REQUEST, Notification.NOT_SEEN);
		NotificationController.deleteNotification(user_id, friend_id, Notification.FRIEND_REQUEST);
		return ok(ResponseWrapper.success());
	}
	
	public static Result denyFriendRequest(Long user_id, Long friend_id) {
		Friend friend_request = Friend.find(friend_id, user_id, Friend.PENDING);
		friend_request.delete();
		
		Friend friend_requested = Friend.find(user_id, friend_id, Friend.CONFIRM);
		friend_requested.delete();
		NotificationController.deleteNotification(user_id, friend_id, Notification.FRIEND_REQUEST);
		return ok(ResponseWrapper.success());
	}
	
	public static Result removeFriend(String friend) {
		Long user_id = (Long) Context.current().args.get("user_id");
		User friend_details = User.findByUsername(friend);
		if(friend_details == null) 
			return ok(ResponseWrapper.notFound("User not found."));
		Friend user_friendship = Friend.find(user_id, friend_details.user_id, Friend.ACCEPTED);
		user_friendship.delete();
		Friend friend_friendship = Friend.find(friend_details.user_id, user_id, Friend.ACCEPTED);
		friend_friendship.delete();
		
		User user = User.find.byId(user_id);
		user.friends_count--;
		user.save();
		User friend1 = User.find.byId(friend_details.user_id);
		friend1.friends_count--;
		friend1.save();
		
		return ok(ResponseWrapper.success());
	}

	public static Result getFriends(String username) {
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		User user = User.findByUsername(username);
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("select * from user join friend on user.user_id = friend.friend"
					+ " where friend.user = ? and friend.status = ?");
			ps.setLong(1, user.user_id);
			ps.setInt(2, Friend.ACCEPTED);
			rs = ps.executeQuery();
		
			while(rs.next()) {
				ObjectNode result = Json.newObject();
				Long friend_id = rs.getLong("user_id");
				result.put("user_id", friend_id);
				result.put("name", rs.getString("name"));
				result.put("username", rs.getString("username"));
				result.put("bio", rs.getString("bio"));
				result.put("profile_picture", rs.getString("profile_picture"));
				result.put("friends_count", rs.getInt("friends_count"));
				result.put("friend_status", Friend.findFriendStatus(user.user_id, friend_id));
				response.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Cannot retrieve friends list."));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
}
	
	public static Result getPendingFriends() {
		Long user_id = (Long) Context.current().args.get("user_id");
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("select * from user join friend on user.user_id = friend.user"
					+ " where friend.friend = ? and friend.status = ?");
			ps.setLong(1, user_id);
			ps.setInt(2, Friend.PENDING);
			rs = ps.executeQuery();
			
			while(rs.next()) {
				ObjectNode result = Json.newObject();
				Long friend_id = rs.getLong("user_id");
				result.put("user_id", friend_id);
				result.put("name", rs.getString("name"));
				result.put("username", rs.getString("username"));
				result.put("bio", rs.getString("bio"));
				result.put("profile_picture", rs.getString("profile_picture"));
				result.put("friends_count", rs.getInt("friends_count"));
				result.put("friend_status", Friend.findFriendStatus(user_id, friend_id));
				response.add(result);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Cannot retrieve pending list."));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
	}
}
