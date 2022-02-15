import com.google.gson.*;
import com.google.gson.annotations.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.*;

public class SteamGuard {

	@SerializedName("shared_secret")
	public String SharedSecret;
	@SerializedName("serial_number")
	public String SerialNumber;
	@SerializedName("revocation_code")
	public String RevocationCode;
	@SerializedName("uri")
	public String URI;
	@SerializedName("server_time")
	public String ServerTime;
	@SerializedName("account_name")
	public String AccountName;
	@SerializedName("token_gid")
	public String TokenGid;
	@SerializedName("identity_secret")
	public String IdentitySecret;
	@SerializedName("secret_1")
	public String Secret1;
	@SerializedName("status")
	public String Status;
	@SerializedName("device_id")
	public String DeviceId;
	@SerializedName("fully_enrolled")
	public String FullyEnrolled;
	public SessionData Session;

	private static byte[] steamGuardCodeTranslations = new byte[] { 50, 51, 52, 53, 54, 55, 56, 57, 66, 67, 68, 70, 71,
			72, 74, 75, 77, 78, 80, 81, 82, 84, 86, 87, 88, 89 };

	public String GenerateSteamGuardCodeForTime(long time) {
		if (this.SharedSecret == null || this.SharedSecret.length() == 0) {
			return "";
		}

		String sharedSecretUnescaped = StringEscapeUtils.unescapeJava(this.SharedSecret);

		byte[] sharedSecretArraySigned = Base64.getDecoder().decode(sharedSecretUnescaped);

		byte[] timeArray = new byte[8];

		time /= 30L;

		for (int i = 8; i > 0; i--) {
			timeArray[i - 1] = (byte) time;
			time >>= 8;
		}
		byte[] hashedData = HmacUtils.hmacSha1(sharedSecretArraySigned, timeArray);
		byte[] codeArray = new byte[5];
		try {
			byte b = (byte) ((byte) hashedData[19] & 0xF);
			int codePoint = (hashedData[b] & 0x7F) << 24 | (hashedData[b + 1] & 0xFF) << 16
					| (hashedData[b + 2] & 0xFF) << 8 | (hashedData[b + 3] & 0xFF);

			for (int i = 0; i < 5; ++i) {
				codeArray[i] = steamGuardCodeTranslations[codePoint % steamGuardCodeTranslations.length];
				codePoint /= steamGuardCodeTranslations.length;
			}
		} catch (Exception ex) {
			return null;
		}
		return new String(codeArray, StandardCharsets.UTF_8);
	}

	public Confirmation[] FetchConfirmations() throws Exception {
		String url = GenerateConfirmationURL();
		Map<String,String> headers = new HashMap<String,String>();
		headers.put("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("User-Agent",
				"Mozilla/5.0 (Linux; U; Android 4.1.1; en-us; Google Nexus 4 - 4.1.1 - API 16 - 768x1280 Build/JRO03S) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
		headers.put("Referer", APIEndpoints.COMMUNITY_BASE);
		Map<String,String> cookies = Session.GetCookies();
		String response = SteamWeb.Request(url, "GET", null, cookies, headers);
		return FetchConfirmationInternal(response);
	}

	private Confirmation[] FetchConfirmationInternal(String response) throws SteamGuard.WGTokenInvalidException {
		String pattern = "<div class=\"mobileconf_list_entry\" id=\"conf[0-9]+\" data-confid=\"(\\d+)\" data-key=\"(\\d+)\" data-type=\"(\\d+)\" data-creator=\"(\\d+)\"";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(response);

		if (response == null || !m.find()) {
			if (response == null || !response.contains("<div>Nothing to confirm</div>")) {
				throw new WGTokenInvalidException();
			}

			return new Confirmation[0];
		}

		List<Confirmation> ret = new ArrayList<>();

		m.reset();
		while (m.find()) {

			if (m.groupCount() != 4)
				continue;
			try {
				BigInteger confID = new BigInteger(m.group(1));
				BigInteger confKey = new BigInteger(m.group(2));
				int confType = Integer.parseInt(m.group(3));
				BigInteger confCreator = new BigInteger(m.group(4));
				ret.add(new Confirmation(confID, confKey, confType, confCreator));
			} catch (NumberFormatException e) {
				continue;
			}
		}
		Confirmation[] array = new Confirmation[ret.size()];
		ret.toArray(array);
		return array;
	}

	public String GenerateConfirmationURL() throws Exception {
		String tag = "conf";
		String endpoint = APIEndpoints.COMMUNITY_BASE + "/mobileconf/conf?";
		String queryString = GenerateConfirmationQueryParams(tag);
		return endpoint + queryString;
	}

	public boolean AcceptConfirmation(Confirmation conf) throws Exception {
		return _sendConfirmationAjax(conf, "allow");
	}

	private boolean _sendConfirmationAjax(Confirmation conf, String op) throws Exception {
		String url = APIEndpoints.COMMUNITY_BASE + "/mobileconf/ajaxop";
		String queryString = "?op=" + op + "&";
		queryString += GenerateConfirmationQueryParams(op);
		queryString += "&cid=" + conf.ID + "&ck=" + conf.Key;
		url += queryString;

		Map<String,String> headers = new HashMap<String,String>();
		headers.put("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("User-Agent",
				"Mozilla/5.0 (Linux; U; Android 4.1.1; en-us; Google Nexus 4 - 4.1.1 - API 16 - 768x1280 Build/JRO03S) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
		headers.put("Referer", APIEndpoints.COMMUNITY_BASE);
		
		Map<String,String> cookies = Session.GetCookies();
		
		String response = SteamWeb.Request(url, "GET", null, cookies, headers);
		if (response == null)
			return false;
		Gson gson = new GsonBuilder().serializeNulls().create();
		SendConfirmationResponse confResponse = gson.fromJson(response, SendConfirmationResponse.class);
		return confResponse.Success;
	}

	public String GenerateConfirmationQueryParams(String tag) throws Exception {
		if (DeviceId != null && DeviceId.isEmpty())
			throw new Exception("Device ID is not present");

		Map<String, String> queryParams = GenerateConfirmationQueryParamsAsNVC(tag);

		return "p=" + queryParams.get("p") + "&a=" + queryParams.get("a") + "&k=" + queryParams.get("k") + "&t="
				+ queryParams.get("t") + "&m=android&tag=" + queryParams.get("tag");
	}

	public Map<String, String> GenerateConfirmationQueryParamsAsNVC(String tag) throws Exception {
		if (DeviceId != null && DeviceId.isEmpty())
			throw new Exception("Device ID is not present");

		long time = TimeAligner.GetSteamTime();
		Map<String, String> ret = new HashMap<String, String>();
		ret.put("p", DeviceId);
		ret.put("a", this.Session.SteamID.toString());
		ret.put("k", _generateConfirmationHashForTime(time, tag));
		ret.put("t", String.valueOf(time));
		ret.put("m", "android");
		ret.put("tag", tag);

		return ret;
	}

	public boolean RefreshSession() throws IOException
    {
        String url = APIEndpoints.MOBILEAUTH_GETWGTOKEN;
        
		Map<String,String> headers = new HashMap<String,String>();
		Map<String,String> data = new HashMap<String,String>();
		data.put("access_token", this.Session.OAuthToken);
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("charset", "utf-8");
		
        String response = null;
        try
        {
            response =  SteamWeb.Request(url,"POST", data,Session.GetCookies(),headers);
        }
        catch (SteamWeb.RequestException ex)
        {
            return false;
        }

        if (response == null) return false;

        try
        {
        	
    		Gson gson = new GsonBuilder().serializeNulls().create();
        	RefreshSessionDataResponse refreshResponse = gson.fromJson(response, RefreshSessionDataResponse.class);
            
            if (refreshResponse == null || refreshResponse.Response == null ||refreshResponse.Response.Token == null|| refreshResponse.Response.Token.isEmpty())
                return false;

            String token = this.Session.SteamID + "%7C%7C" + refreshResponse.Response.Token;
            String tokenSecure = this.Session.SteamID + "%7C%7C" + refreshResponse.Response.TokenSecure;

            this.Session.SteamLogin = token;
            this.Session.SteamLoginSecure = tokenSecure;
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

	public String _generateConfirmationHashForTime(long time, String tag) {
		byte[] decode = Base64.getDecoder().decode(this.IdentitySecret);
		int n2 = 8;
		if (tag != null) {
			if (tag.length() > 32) {
				n2 = 8 + 32;
			} else {
				n2 = 8 + tag.length();
			}
		}

		byte[] array = new byte[n2];
		int n3 = 8;
		while (true) {
			int n4 = n3 - 1;
			if (n3 <= 0) {
				break;
			}
			array[n4] = (byte) time;
			time >>= 8;
			n3 = n4;
		}

		if (tag != null) {
			try {
				array = ArrayUtils.addAll(Arrays.copyOf(array, 8), tag.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		try {
			byte[] hashedData = HmacUtils.hmacSha1(decode, array);
			String encodedData = Base64.getEncoder().encodeToString(hashedData);
			String hash = URLEncoder.encode(encodedData);
			return hash;
		} catch (Exception e) {
			return null;
		}
	}

	private class SendConfirmationResponse {
		@SerializedName("success")
		public boolean Success;
	}

	private class RefreshSessionDataResponse {
		@SerializedName("response")
		public RefreshSessionDataInternalResponse Response;

		class RefreshSessionDataInternalResponse {
			@SerializedName("token")
			public String Token;

			@SerializedName("token_secure")
			public String TokenSecure;
		}
	}

	public static class WGTokenInvalidException extends Exception {

	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().serializeNulls().create();
		String mafile = gson.toJson(this);
		return mafile;
	}

}
