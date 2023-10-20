package io.mosip.kernel.smsserviceprovider.nimba.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.simple.JSONObject;
import io.mosip.kernel.core.exception.UnsupportedEncodingException;
import io.mosip.kernel.smsserviceprovider.nimba.constant.SmsPropertyConstant;

/**
 * Sending SMS using NIMBA APIS
 * @author condeis
 *
 */
public class NimbaMessageRequest {

	@SuppressWarnings("unchecked")
	public static void send(String apiurl, String authorization, String senderId, String contact, String message)
			throws UnsupportedEncodingException, MalformedURLException, IOException, JSONException {
	//	message = URLEncoder.encode(message, "ISO-8859-1");
		HttpURLConnection urlConnection = (HttpURLConnection) new URL(apiurl).openConnection(); // OK
		urlConnection.setDoOutput(true); // Triggers POST.
		urlConnection.setDoInput(true);
		urlConnection.setRequestMethod("POST");
		urlConnection.setRequestProperty("Authorization", authorization);
		urlConnection.setRequestProperty("Content-Type", "application/json");
		JSONObject jsonParam = new JSONObject();
		List<String> to = new ArrayList<String>();
		to.add(contact);
		jsonParam.put(SmsPropertyConstant.RECIPIENT_NUMBER.getProperty(), to);
		jsonParam.put(SmsPropertyConstant.SENDER_ID.getProperty(), senderId);
		jsonParam.put(SmsPropertyConstant.CONTENT_MESSAGE.getProperty(), message);
		OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
		wr.write(jsonParam.toString());
		wr.flush();
		BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

	}

}
