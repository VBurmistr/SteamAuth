import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class SteamWeb {

	public static String MobileLoginRequest(String url, String method, Map<String, String> data,
			Map<String, String> cookies, Map<String, String> headers) throws IOException, SteamWeb.RequestException {
		return Request(url, method, data, cookies, headers);
	}

	public static String Request(String urlStr, String method, Map<String, String> data, Map<String, String> cookies,
			Map<String, String> headers) throws IOException, SteamWeb.RequestException {		
		URL url = new URL(urlStr);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		
		if (headers != null) {
			for (Map.Entry<String, String> header : headers.entrySet()) {
				http.setRequestProperty(header.getKey(), header.getValue());
			}
		}else {
			http.setRequestProperty("Referer", APIEndpoints.COMMUNITY_BASE);			
		}
		
		String cookiesString = "";
		if (cookies != null) {
			for (Map.Entry<String, String> cookie : cookies.entrySet()) {
				cookiesString = String.join(";", cookie.getKey() + "=" + cookie.getValue(), cookiesString);
			}
			http.setRequestProperty("Cookie", cookiesString);			

		}
		http.setRequestMethod(method);
		http.setInstanceFollowRedirects(false);	
		
		String sendData = "";
		if (data != null) {
			http.setDoOutput(true);
			for (Map.Entry<String, String> dataEntry : data.entrySet()) {
				sendData = String.join("&", dataEntry.getKey() + "=" + URLEncoder.encode(dataEntry.getValue(), StandardCharsets.UTF_8.toString()), sendData);
			}
			
			sendData = sendData.substring(0, sendData.length() - 1);
			http.setRequestProperty("Content-Length", Integer.toString(sendData.length()));

			try (DataOutputStream wr = new DataOutputStream(http.getOutputStream())) {
				wr.write(sendData.getBytes("UTF-8"));
			}
		}

		String response = new String();
		BufferedReader br = null;
		if (http.getResponseCode() == 302 && http.getHeaderField("Location").equals("steammobile://lostauth")) {
			throw new RequestException(http.getResponseCode(), http.getHeaderField("Location"));
		} else if (100 <= http.getResponseCode() && http.getResponseCode() <= 399) {
			if ("gzip".equals(http.getContentEncoding())) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(http.getInputStream())));
			} else {
				br = new BufferedReader(new InputStreamReader(http.getInputStream(), "UTF-8"));
			}
			for (String line; (line = br.readLine()) != null; response += line);

		} else {
			if ("gzip".equals(http.getContentEncoding())) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(http.getErrorStream())));
			} else {
				br = new BufferedReader(new InputStreamReader(http.getErrorStream(), "UTF-8"));
			}
			for (String line; (line = br.readLine()) != null; response += line)
				;
			throw new RequestException(http.getResponseCode(), response);
		}
		
		return response;
	}


	public static class RequestException extends Exception {
		int respCode;
		String responseText;

		public RequestException(int code, String responseText) {
			super(String.valueOf(code));
			this.respCode = code;
			this.responseText = responseText;

		}
	}

}
