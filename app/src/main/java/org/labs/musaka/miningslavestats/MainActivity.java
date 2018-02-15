package org.labs.musaka.miningslavestats;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private TextView status;
    private TextView hashrate;
    private TextView balance;
    private TimerTask timerTask;
    private Timer timer;
    private boolean progressMassageShown = false;

    private String userAdress;
    private final String GENERAL_INFO_NANOPOOL_ADRESS = "https://api.nanopool.org/v1/eth/user/";


    //find changes pls
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);

        //Initialise the default preferences for when the user has not set any
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        userAdress = sharedPrefs.getString(getString(R.string.accountAddressKey),null);


        status = findViewById(R.id.tv_status_data);
        hashrate = findViewById(R.id.tv_hashrate_speed);
        balance = findViewById(R.id.tv_balance_data);


        ScheduleAsyncTask();
    }


    private void ScheduleAsyncTask() {
        final Handler handler = new Handler(); //This is used to call the asyncTask from th main thread as Timer is running on it's own thread
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("syncing","Starting the AsyncTask");
                        new NanopoolAPI().execute(createUserSpecificURL(userAdress));
                    }
                });
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask,10,30000); //todo maybe incorporate the period in a setting?
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


    public void startSttings() {
        Intent intent = new Intent(this,SettingsActivity.class);
        startActivity(intent);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startSttings();
                break;
            case  R.id.menuitem_refresh:
                Log.d("refreshing","Starting the AsyncTask");
                new NanopoolAPI().execute(createUserSpecificURL(userAdress));
                break;
            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private class NanopoolAPI extends AsyncTask<URL ,Void ,String> {
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);


        @Override
        protected void onPreExecute() {
            //todo change this to an animation for the balance
            if (!progressMassageShown) {
                progressMassageShown = true;
                progressDialog.setMessage("Downloading your data...");
                progressDialog.show();
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface arg0) {
                        NanopoolAPI.this.cancel(true);
                    }
                });
            }
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

    @Override
    protected void onPause() {
        timerTask.cancel();
        progressMassageShown = false;
        super.onPause();
    }

    @Override
    protected void onRestart() {
        ScheduleAsyncTask();
        super.onRestart();
    }
}
