package com.michaeldpalmer.morrisclocking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements Button.OnClickListener {

	private String url = "http://timeips.morristechnology.com/remote/index.php";
	private TextView tv, employee_name, stateMessage, hoursWorked, hoursRemaining;
	private Button btnClockIn, btnClockOut;
	private DownloadDataTask dlTask = null;
	private String prefUser, prefPass, submitName = "checkStatus", submitValue = "Check Status";
	private WifiManager wifiMgr;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Buttons
		Button btnStatus = (Button) findViewById(R.id.status_btn);
		btnClockIn = (Button) findViewById(R.id.clock_in_btn);
		btnClockOut = (Button) findViewById(R.id.clock_out_btn);
		btnClockIn.setOnClickListener(this);
		btnClockOut.setOnClickListener(this);
		btnStatus.setOnClickListener(this);

		// TextViews
		tv = (TextView) findViewById(R.id.txtHello);
		employee_name = (TextView) findViewById(R.id.employee_name);
		stateMessage = (TextView) findViewById(R.id.stateMessage);
		hoursWorked = (TextView) findViewById(R.id.hoursWorked);
		hoursRemaining = (TextView) findViewById(R.id.hoursRemaining);

		// Preferences
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		prefUser = sharedPref.getString(SettingsActivity.KEY_PREF_CRED_USER, "");
		prefPass = sharedPref.getString(SettingsActivity.KEY_PREF_CRED_PASS, "");

		// Check wifi
		wifiMgr = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		if(!checkWifiStatus())
			return;

		// Get current status
		if (dlTask != null && dlTask.getStatus() != DownloadDataTask.Status.FINISHED)
			dlTask.cancel(true);

		dlTask = (DownloadDataTask) new DownloadDataTask().execute(url);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.clock_in_btn:
				submitName = "clockIn";
				submitValue = "Clock In";
				break;
			case R.id.clock_out_btn:
				submitName = "clockOut";
				submitValue = "Clock Out";
				break;
			case R.id.status_btn:
				submitName = "checkStatus";
				submitValue = "Check Status";
				break;
			default:
				return;
		}

		if(checkWifiStatus()) {
			Toast.makeText(this, submitValue + "...", Toast.LENGTH_SHORT).show();

			if (dlTask != null && dlTask.getStatus() != DownloadDataTask.Status.FINISHED)
				dlTask.cancel(true);

			dlTask = (DownloadDataTask) new DownloadDataTask().execute(url);
		}
	}

	private boolean checkWifiStatus() {
		if(!wifiMgr.isWifiEnabled()) {
			employee_name.setText(getString(R.string.enableWifi));
			Toast.makeText(this, getString(R.string.wifiDisabled), Toast.LENGTH_LONG).show();
			btnClockIn.setEnabled(false);
			btnClockOut.setEnabled(false);
			stateMessage.setVisibility(View.GONE);
			hoursWorked.setVisibility(View.GONE);
			hoursRemaining.setVisibility(View.GONE);
			tv.setVisibility(View.GONE);
			return false;
		} else {
			WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
			String ssid = wifiInfo.getSSID();
			if(!ssid.equals("\"NOC\"")) {
				employee_name.setText( getString(R.string.connectNOC) );
				btnClockIn.setEnabled(false);
				btnClockOut.setEnabled(false);
				stateMessage.setVisibility(View.GONE);
				hoursWorked.setVisibility(View.GONE);
				hoursRemaining.setVisibility(View.GONE);
				tv.setVisibility(View.GONE);
				Toast.makeText(this, "Connect to network \"NOC\"", Toast.LENGTH_LONG).show();
				return false;
			}
		}

		stateMessage.setVisibility(View.VISIBLE);
		hoursWorked.setVisibility(View.VISIBLE);
		hoursRemaining.setVisibility(View.VISIBLE);
		tv.setVisibility(View.VISIBLE);
		return true;
	}


	private class DownloadDataTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {
			String response = "";
			try {
				// Set parameters
				URL url = new URL(urls[0]);
				Map<String,Object> params = new LinkedHashMap<>();
				params.put("username", prefUser);
				params.put("password", prefPass);
				params.put(submitName, submitValue);

				// Build POST data
				StringBuilder postData = new StringBuilder();
				for (Map.Entry<String,Object> param : params.entrySet()) {
					if (postData.length() != 0) postData.append('&');
					postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
					postData.append('=');
					postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
				}

				// Perform connection
				String urlParameters = postData.toString();
				URLConnection conn = url.openConnection();
				conn.setDoOutput(true);

				// Write output
				OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
				writer.write(urlParameters);
				writer.flush();

				// Read output
				String line;
				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				while ((line = reader.readLine()) != null) {
					response += line;
				}

				// Close reader and writer
				writer.close();
				reader.close();

			} catch (Exception e) {
				Log.e("DownloadData", "Error encountered while downloading data");
				e.printStackTrace();
			}
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			// Check for cancellations
			if (isCancelled())
				result = null;

			// Check result
			if (result != null) {
				Pattern p;
				Matcher m;
				String name = "", state = "", time = "", worked = "", remaining = "";
				if(submitName.equals("clockIn") || submitName.equals("clockOut")) {
					p = Pattern.compile(
							"<b>([a-z,. -]+)</b>.*(in|out) at (\\d+:\\d+ [ap]m).*worked (.*) and have (.*) remaining.*",
							Pattern.CASE_INSENSITIVE
					);
					m = p.matcher(result);
					while (m.find()) {
						name = m.group(1);
						state = m.group(2).toLowerCase();
						time = m.group(3);
						worked = m.group(4)
								  .replace("<br/>", " ")
								  .replaceAll("<span[^>]*>([^<]*)</span>", " $1")
								  .replace("</span>", "");
						remaining = m.group(5)
									 .replace("<br/>", " ")
									 .replaceAll("<span[^>]*>([^<]*)</span>", " $1")
									 .replace("</span>", "");
					}
					employee_name.setText(name);
					stateMessage.setText(String.format("Clocked %s at %s", state, time));
					hoursWorked.setText(getString(R.string.hoursWorked, worked));
					hoursRemaining.setText(getString(R.string.hoursRemaining, remaining));
					tv.setText(result);
				} else {
					p = Pattern.compile(
							"<b>([a-z,. -]+)</b>.*(in|out).*worked (.*) and have (.*) remaining.*",
							Pattern.CASE_INSENSITIVE
					);
					m = p.matcher(result);
					while (m.find()) {
						name = m.group(1);
						state = m.group(2).toLowerCase();
						worked = m.group(3)
								  .replace("<br/>", " ")
								  .replaceAll("<span[^>]*>([^<]*)</span>", " $1")
								  .replace("</span>", "");
						remaining = m.group(4)
									 .replace("<br/>", " ")
									 .replaceAll("<span[^>]*>([^<]*)</span>", " $1")
									 .replace("</span>", "");
					}
					employee_name.setText(name);
					stateMessage.setText(String.format("Clocked %s", state));
					hoursWorked.setText(getString(R.string.hoursWorked, worked));
					hoursRemaining.setText(getString(R.string.hoursRemaining, remaining));
					tv.setText(result);
				}

				if (state.equals("out")) {
					btnClockIn.setEnabled(true);
					btnClockOut.setEnabled(false);
				} else if (state.equals("in")) {
					btnClockIn.setEnabled(false);
					btnClockOut.setEnabled(true);
				}
			}
		}
	}
}
