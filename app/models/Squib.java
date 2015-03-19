package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import play.data.format.Formats;
import play.db.ebean.Model;

@Entity
public class Squib extends Model {

	private static final long serialVersionUID = 1L;
	public final static int VALID = 1;
	public final static int INVALID = 0;
	public final static int PUBLIC = 1;
	public final static int PRIVATE = 0;

	@Id
	public long post_id;
	
	@Version 
	public int version;
	
	@Formats.NonEmpty
	public long time_created;
	
	@Formats.NonEmpty
	public long time_expired;
	
	@Formats.NonEmpty
	public long user_id;
	
	@Formats.NonEmpty
	public int validation;
	
	@Formats.NonEmpty
	public int access;
	
	@Formats.NonEmpty
	public float longitude;
	
	@Formats.NonEmpty
	public float latitude;
	
	@Formats.NonEmpty
	public String post;
	
	public String picture;

	public static Model.Finder<Long, Squib> find = new Model.Finder<Long, Squib>(Long.class, Squib.class);
	
	public static List<Squib> searchSquibs(String user_id) {
		return find.where().eq("user_id", user_id).orderBy("time_created").findList();
	}
}
