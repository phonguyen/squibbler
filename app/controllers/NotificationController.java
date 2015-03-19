package controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.Friend;
import models.Notification;
import models.ResponseWrapper;
import models.User;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.With;
import util.AuthenticateRequest;
import util.DatabaseManager;

@With(AuthenticateRequest.class)
public class NotificationController extends Controller {

	public static Result insertNotification(ArrayList<Long> friend_ids, int code, int seen) {
		Long user_id = (Long) Context.current().args.get("user_id");
		Long timestamp = System.currentTimeMillis() / 1000l;
		for(Long friend_id : friend_ids) {
			Notification notification = new Notification();
			notification.receiver_id = friend_id;
			notification.sender_id = user_id;
			notification.code = code;
			notification.timestamp = timestamp;
			notification.seen = seen;
			notification.save();
		}
		return ok(ResponseWrapper.success());
	}
	
	public static Result insertNotification(Long friend_id, int code, int seen) {
		Long user_id = (Long) Context.current().args.get("user_id");
		Notification notification = new Notification();
		notification.receiver_id = friend_id;
		notification.sender_id = Long.valueOf(user_id);
		notification.code = code;
		notification.timestamp = System.currentTimeMillis() / 1000l;
		notification.seen = seen;
		notification.save();
		return ok(ResponseWrapper.success());
	}
	
	public static Result deleteNotification(Long user_id, Long friend_id, int code) {
		Notification notification = Notification.findNotification(String.valueOf(user_id), String.valueOf(friend_id), 
				String.valueOf(code));
		notification.delete();
		return ok(ResponseWrapper.success());
	}
	
	public static Result getNotifications() {
		Long user_id = (Long) Context.current().args.get("user_id");
		ArrayNode response = JsonNodeFactory.instance.arrayNode();
		Connection con = DB.getConnection();
		PreparedStatement ps = null;
		ResultSet rs =null;
		Map<Long, User> userMap = new HashMap<Long, User>();
		try { 
			ps = con.prepareStatement("select * from user join notification on user.user_id = notification.sender_id "
					+ "where notification.receiver_id = ?");
			ps.setLong(1, user_id);
			rs = ps.executeQuery();
			while(rs.next()) {
				User user = new User();
				user.user_id = rs.getLong("user_id");
				user.username = rs.getString("username");
				user.name = rs.getString("name");
				user.profile_picture = rs.getString("profile_picture");
				user.bio = rs.getString("bio");
				user.friends_count = rs.getInt("friends_count");
				userMap.put(user.user_id, user);
			}
			List<Notification> notifications = Notification.getNotifications(String.valueOf(user_id));
			for(Notification notification : notifications) {
				ObjectNode result = Json.newObject();
				User user = userMap.get(notification.sender_id);
				result.put("user", Json.toJson(user));
				result.put("friend_status", Friend.findFriendStatus(user_id, user.user_id));
				result.put("notification", Json.toJson(notification));
				response.add(result);
			}
		} catch(SQLException e) {
			e.printStackTrace();
			return ok(ResponseWrapper.badRequest("Cannot retrieve notifications."));
		} finally {
			DatabaseManager.close(rs, ps, con);
		}
		return ok(ResponseWrapper.success(response));
	}
}
