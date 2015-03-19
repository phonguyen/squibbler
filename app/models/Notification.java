package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import play.data.format.Formats;

@Entity
public class Notification extends Model {

	private static final long serialVersionUID = 1L;
	final static public int NOT_SEEN = 0;
	final static public int SEEN = 1;
	
	final static public int FRIEND_REQUEST = 300;
	final static public int ACCEPTED_FRIEND_REQUEST = 301;
	
	@Id
	public long notification_id;
	
	@Formats.NonEmpty
	public long receiver_id;
	
	@Formats.NonEmpty
	public long sender_id;
	
	@Formats.NonEmpty
	public int code;
	
	@Formats.NonEmpty
	public long timestamp;
	
	@Formats.NonEmpty
	public int seen;
	
	public static Model.Finder<Long, Notification> find = new Model.Finder<Long, Notification>(Long.class, Notification.class);
	
	public static Notification findNotification(String receiver_id, String sender_id, String code) {
		return find.where().eq("receiver_id", receiver_id).eq("sender_id", sender_id).eq("code", code).findUnique();
	}
	
	public static List<Notification> getNotifications(String user_id) {
		return find.where().eq("receiver_id", user_id).findList();
	}
}
