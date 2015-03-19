import models.Token;
import play.*;

public class Global extends GlobalSettings {

	public void onStart(Application app) {
		Token.init();
		//Cassandra.connect("127.0.0.1");
	}
	
	public void onStop(Application app) {
		//Cassandra.close();
	}
}
