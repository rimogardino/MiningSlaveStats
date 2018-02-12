package org.labs.musaka.miningslavestats;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private TextView status;
    private TextView hashrate;
    private TextView balance;
    private String results;

    private String userAdress = "0xdd4417c91cb817fabead55aef7f43dec189c5f2c";
    private final String GENERAL_INFO_NANOPOOL_ADRESS = "https://api.nanopool.org/v1/eth/user/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.tv_status_data);
        hashrate = findViewById(R.id.tv_hashrate_speed);
        balance = findViewById(R.id.tv_balance_data);

        new NanopoolAPI().execute(createUserSpecificURL(userAdress));
    }



    private URL createUserSpecificURL(String accountAddress){
        Uri uriBuilder = Uri.parse(GENERAL_INFO_NANOPOOL_ADRESS).buildUpon()
                .appendPath(accountAddress)
                .build();

        URL mainURL = null;

        try {
             mainURL = new URL(uriBuilder.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return mainURL;
    }



    private class NanopoolAPI extends AsyncTask<URL ,Void ,String> {
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);


        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Downloading your data...");
            progressDialog.show();
            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface arg0) {
                    NanopoolAPI.this.cancel(true);
                }
            });
        }

        @Override
        protected String doInBackground(URL... urls) {
            String jsonString = null;
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) urls[0].openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                InputStream in = urlConnection.getInputStream();

                Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\\A");

                boolean hasInput = scanner.hasNext();
                if (hasInput) {
                    jsonString = scanner.next();
                } else {
                    return "could't load JSON";
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }

            return jsonString;
        }


        @Override
        protected void onPostExecute(String s) {
            JSONObject apiJSONobj = null;
            JSONObject JSONdata = null;


            try {
                apiJSONobj = new JSONObject(s);
                JSONdata = apiJSONobj.getJSONObject("data");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            try {
                if (JSONdata != null) {
                    balance.setText(String.valueOf(JSONdata.getDouble("balance")) + " ETH");
                    double curenthashrate = JSONdata.getDouble("hashrate");
                    hashrate.setText(String.valueOf(curenthashrate));

                    if (curenthashrate == 0) {
                        status.setText("Dead");
                    } else {
                        status.setText("Live");
                    }

                }

                this.progressDialog.dismiss();


            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

}
