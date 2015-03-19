package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import play.data.validation.Constraints;
import play.data.format.Formats;
import play.db.ebean.Model;

@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user", "friend"}))
@Entity
public class Friend extends Model {

	private static final long serialVersionUID = 1L;
	public final static int NOT_FRIENDS = 0;
	public final static int PENDING = 1;
	public final static int CONFIRM = 2;
	public final static int ACCEPTED = 3;
	
	@Id 
	public long friends_id;
	
	@Constraints.Required
	@Formats.NonEmpty
	public long user;
	
	@Constraints.Required
	@Formats.NonEmpty
	public long friend;
	
	@Constraints.Required
	@Formats.NonEmpty
	public int status;
	
	public static Model.Finder<Long, Friend> find = new Model.Finder<Long, Friend>(Long.class, Friend.class);
	
	public static Friend find(Long user, Long friend, int status) {
		return find.where().eq("user", user).eq("friend", friend).eq("status", status).findUnique();
	}
	
	public static Friend findByFriend(String friend) {
		return find.where().eq("friend", friend).findUnique();
	}
	
	public static int findFriendCount(Long user_id) {
		return find.where().eq("user", user_id).eq("status", ACCEPTED).findRowCount();
	}
	
	public static int findFriendStatus(Long user_id, Long friend_id) {
		Friend friend = find.where().eq("user", user_id).eq("friend", friend_id).findUnique();
		if(friend == null) {
			return NOT_FRIENDS;
		}
		return friend.status;
	}
}
