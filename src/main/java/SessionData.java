import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SessionData {
	public String SessionID;
	public String SteamLogin;
	public String SteamLoginSecure;
	public String WebCookie;
	public String OAuthToken;
	public Long SteamID;

	public Map<String,String> GetCookies() {
		Map<String,String> cookieContainer = new HashMap<String,String>();
		cookieContainer.put("dob","");
		cookieContainer.put("mobileClientVersion","0 (2.1.3)");
		cookieContainer.put("mobileClient","android");
		cookieContainer.put("steamid",SteamID.toString());
		cookieContainer.put("steamLogin",SteamLogin);
		cookieContainer.put("steamLoginSecure",SteamLoginSecure);
		cookieContainer.put("Steam_Language","english");
		cookieContainer.put("sessionid", this.SessionID);
		return cookieContainer;

	}
	
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().serializeNulls().create();
		String mafile = gson.toJson(this);
		return mafile;
	}

}
