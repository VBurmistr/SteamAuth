import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.*;

public class UserLogin {
	public String Username;
	public String Password;
	public long SteamID;

	public boolean RequiresCaptcha;
	public String CaptchaGID = null;
	public String CaptchaText = null;

	public boolean RequiresEmail;
	public String EmailDomain = null;
	public String EmailCode = null;

	public boolean Requires2FA;
	public String TwoFactorCode = null;

	public SessionData Session = null;
	public boolean LoggedIn = false;

	Map<String, String> Cookies = new HashMap<String, String>();


	public UserLogin(String username, String password) {
		this.Username = username;
		this.Password = password;
	}

	public LoginResult DoLogin() throws IOException, InterruptedException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, SteamWeb.RequestException
    {
        Map<String,String> postData = new HashMap<String,String>();
        
        Map<String, String> cookies = Cookies;
        String response = null;

        if (cookies.size() == 0)
        {
        	cookies.put("mobileClientVersion", "0 (2.1.3)");
        	cookies.put("mobileClient", "android");
        	cookies.put("Steam_Language", "english");
        	
            Map<String, String> headers = new HashMap<String, String>();          

            headers.put("X-Requested-With", "com.valvesoftware.android.steam.community");
            headers.put("Referer", APIEndpoints.COMMUNITY_BASE + "/mobilelogin?oauth_client_id=DE45CD61&oauth_scope=read_profile%20write_profile%20read_client%20write_client");
            String getSessionResp = SteamWeb.MobileLoginRequest("https://steamcommunity.com/login?oauth_client_id=DE45CD61&oauth_scope=read_profile%20write_profile%20read_client%20write_client", "GET", null, cookies, headers);
               
            String pattern = "g_sessionID = \"([\\d\\w]{10,})";
            Pattern r = Pattern.compile(pattern);
    		Matcher m = r.matcher(getSessionResp);

    		if (getSessionResp == null || !m.find()) {
    			return LoginResult.GeneralFailure;
    		}else {
        		m.reset();
        		m.find();
				String sessionId = m.group(1);
				cookies.put("sessionid",sessionId);
    			
    		}
  
        
        }

        postData.put("donotcache", String.valueOf(TimeAligner.GetSteamTime() * 1000));
        postData.put("username", this.Username);
        response = SteamWeb.MobileLoginRequest(APIEndpoints.COMMUNITY_BASE + "/login/getrsakey", "POST", postData, cookies, null);
        if (response == null || response.contains("<BODY>\nAn error occurred while processing your request.")) return LoginResult.GeneralFailure;

        
        Gson gson = new GsonBuilder().serializeNulls().create();
        RSAResponse rsaResponse = gson.fromJson(response, RSAResponse.class);
        
        if (!rsaResponse.Success)
        {
            return LoginResult.BadRSA;
        }

        Thread.sleep(350);

        String encryptedPassword = Util.RsaEncrypt(rsaResponse.Modulus,rsaResponse.Exponent,this.Password);

        postData.clear();
        postData.put("donotcache", String.valueOf(TimeAligner.GetSteamTime() * 1000));
        postData.put("password", encryptedPassword);
        postData.put("username", this.Username);     
        postData.put("twofactorcode", this.TwoFactorCode!=null?this.TwoFactorCode:"");
        postData.put("emailauth", this.RequiresEmail ? this.EmailCode : "");
        postData.put("loginfriendlyname", "");
        postData.put("captchagid", this.RequiresCaptcha ? this.CaptchaGID : "-1");
        postData.put("captcha_text", this.RequiresCaptcha ? this.CaptchaText : "");
        postData.put("emailsteamid", (this.Requires2FA || this.RequiresEmail) ? String.valueOf(this.SteamID) : "");
        postData.put("rsatimestamp", rsaResponse.Timestamp);
        postData.put("remember_login", "true");
        postData.put("oauth_client_id", "DE45CD61");
        postData.put("oauth_scope", "read_profile write_profile read_client write_client");

        response = SteamWeb.MobileLoginRequest(APIEndpoints.COMMUNITY_BASE + "/login/dologin", "POST", postData, cookies,null);
        if (response == null) return LoginResult.GeneralFailure;

        LoginResponse loginResponse = gson.fromJson(response, LoginResponse.class);
        if (loginResponse.Message != null)
        {
            if(loginResponse.Message.contains("There have been too many login failures"))
                return LoginResult.TooManyFailedLogins;

            if(loginResponse.Message.contains("Incorrect login"))
                return LoginResult.BadCredentials;
        }

        if (loginResponse.CaptchaNeeded)
        {
            this.RequiresCaptcha = true;
            this.CaptchaGID = loginResponse.CaptchaGID;
            return LoginResult.NeedCaptcha;
        }

        if (loginResponse.EmailAuthNeeded)
        {
            this.RequiresEmail = true;
            this.SteamID = loginResponse.EmailSteamID;
            return LoginResult.NeedEmail;
        }

        if (loginResponse.RequiresTwofactor && !loginResponse.Success)
        {
            this.Requires2FA = true;
            return LoginResult.Need2FA;
        }

        if (loginResponse._Oauth == null )
        {
            return LoginResult.GeneralFailure;
        }

        if (!loginResponse.LoginComplete)
        {
            return LoginResult.BadCredentials;
        }
        else
        {
        	Oauth oAuthData = gson.fromJson(loginResponse._Oauth, Oauth.class) ;
        	if(oAuthData.OauthToken == null || oAuthData.OauthToken.length() == 0) {
        		
                return LoginResult.GeneralFailure;

        	}

        	
            SessionData session = new SessionData();
            session.OAuthToken = oAuthData.OauthToken;
            session.SteamID = Long.parseLong(oAuthData.SteamId);
            session.SteamLogin = oAuthData.SteamId + "%7C%7C" + oAuthData.SteamLogin;
            session.SteamLoginSecure = oAuthData.SteamId + "%7C%7C" + oAuthData.SteamLoginSecure;
            session.WebCookie = oAuthData.WebCookie;
            session.SessionID = cookies.get("sessionid");
            this.Session = session;
            this.LoggedIn = true;
            return LoginResult.LoginOkay;
        }
    }

	private class LoginResponse {

		
        @SerializedName("message")
        public String Message;
		@SerializedName("success")
		public boolean Success;
		@SerializedName("login_complete")
		public boolean LoginComplete;
		@SerializedName("captcha_gid")
	    public String CaptchaGID;
		@SerializedName("emailsteamid")
	    public long EmailSteamID;
        @SerializedName("emailauth_needed")
        public boolean EmailAuthNeeded;
		@SerializedName("captcha_needed")
	    public boolean CaptchaNeeded;
		@SerializedName("requires_twofactor")
		public boolean RequiresTwofactor;
		@SerializedName("oauth")
		public String _Oauth;
		

	}
	
	
	public class Oauth {
		@SerializedName("oauth_token")
		public String OauthToken;
		@SerializedName("wgtoken")
		public String SteamLogin;
		@SerializedName("steamid")
		public String SteamId;
		@SerializedName("wgtoken_secure")
		public String SteamLoginSecure;
		@SerializedName("webcookie")
		public String WebCookie;
		@SerializedName("account_name")
		public String AccountName;
	}
	
	private class RSAResponse {
		@SerializedName("success")
		public boolean Success;
		@SerializedName("publickey_exp")
		public String Exponent;
		@SerializedName("publickey_mod")
		public String Modulus;
		@SerializedName("timestamp")
		public String Timestamp;
		@SerializedName("token_gid")
		public String TokenGid;
	}
	
	
	public enum LoginResult {
		LoginOkay, GeneralFailure, BadRSA, BadCredentials, NeedCaptcha, Need2FA, NeedEmail, TooManyFailedLogins,
	}

}
