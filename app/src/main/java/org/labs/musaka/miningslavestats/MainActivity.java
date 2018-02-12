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
            JSONArray JSONworkers = null;

            try {
                apiJSONobj = new JSONObject(s);
                JSONdata = apiJSONobj.getJSONObject("data");
                JSONworkers = JSONdata.getJSONArray("workers");
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

                if (JSONworkers != null) {
                    Object workersMaybe = JSONworkers.get(0);

                    Log.d("tagchetoMI",workersMaybe.toString());
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

























/*    public String getAPIJAson(final URL url) throws IOException {
        final String[] resultz = new String[1];

        Runnable runnable = new Runnable() {
            @Override
            public void run()  {
                HttpURLConnection urlConnection = null;
                try {
                    urlConnection = (HttpURLConnection) url.openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    InputStream in = urlConnection.getInputStream();

                    Scanner scanner = new Scanner(in);
                    scanner.useDelimiter("\\A");

                    boolean hasInput = scanner.hasNext();
                    if (hasInput) {
                        resultz[0] = scanner.next();
                    } else {
                        resultz[0] = null;
                    }


                    results = resultz[0];
                    status.post(new Runnable() {
                        @Override
                        public void run() {

                            JSONObject obj = null;

                            try {

                                obj = new JSONObject(results);

                                Log.d("My App", obj.toString());

                            } catch (Throwable t) {
                                Log.e("My App", "Could not parse malformed JSON: \"" + results + "\"");
                            }




                            try {
                                String statusText = obj.getBoolean("status") == true?"Live":"Dead";
                                status.setText(statusText);

                                JSONObject arrayche = obj.getJSONObject("data");
                                balance.setText(String.valueOf(arrayche.getDouble("balance")));
                                hashrate.setText(String.valueOf(arrayche.getDouble("hashrate")));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }


                    });
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            }
        };

        new Thread(runnable).start();

        return resultz[0];

    }*/



}
