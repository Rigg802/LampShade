package com.kuxhausen.huemore.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.kuxhausen.huemore.persistence.DatabaseDefinitions.PreferencesKeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;

public class SynchronousTransmitGroupMood {
	/**
	 * synchronous version of TransmitGroupMood
	 * @param cont
	 * @param bulbs
	 * @param moods
	 * @return
	 */
	public Integer execute(Context cont, Integer[] bulbs, String[] moods){
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

		StrictMode.setThreadPolicy(policy); 
		
		if (cont == null || bulbs == null || moods == null)
			return -1;

		// Get username and IP from preferences cache
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(cont);
		String bridge = settings.getString(PreferencesKeys.BRIDGE_IP_ADDRESS,
				null);
		String hash = settings.getString(PreferencesKeys.HASHED_USERNAME, "");

		if (bridge == null)
			return -1;

		for (int i = 0; i < bulbs.length; i++) {

			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();

			HttpPut httpPut = new HttpPut("http://" + bridge + "/api/" + hash
					+ "/lights/" + bulbs[i] + "/state");
			try {

				StringEntity se = new StringEntity(moods[i % moods.length]);

				// sets the post request as the resulting string
				httpPut.setEntity(se);

				HttpResponse response = client.execute(httpPut);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {

					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(content));
					String line;
					String debugOutput = "";
					while ((line = reader.readLine()) != null) {
						builder.append(line);
						debugOutput += line;
					}
				} else {
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return 1;
	}
}