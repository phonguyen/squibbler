package util;

import models.ResponseWrapper;
import models.Token;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.*;

public class AuthenticateRequest extends Action.Simple {

	public final static String AUTH_TOKEN_HEADER = "Authorization"; 
	
	public Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
		String token = "";
		String[] authTokenHeaderValues = ctx.request().headers().get(AUTH_TOKEN_HEADER);
		if((authTokenHeaderValues != null) && (authTokenHeaderValues.length == 1) && (authTokenHeaderValues[0] != null)) {
			token = authTokenHeaderValues[0];
			Token authToken = Token.findByToken(token);
			if(authToken != null) {
				ctx.args.put("user_id", authToken.user_id);
				return delegate.call(ctx);
			} else {
				SimpleResult unauthorized = Results.unauthorized(ResponseWrapper.unauthorized("Invalid token."));
				return F.Promise.pure(unauthorized);
			}
		}
		SimpleResult noToken = Results.unauthorized(ResponseWrapper.unauthorized("No authorization token provided."));
		return F.Promise.pure(noToken);
	}
}
