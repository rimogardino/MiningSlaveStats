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
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    private TextView status;
    private TextView hashrate;
    private TextView balance;
    private Spinner curenciesSpinner;
    private TimerTask timerTask;
    private Timer timer;
    private NumberFormat formatterForETH = new DecimalFormat("#0.00000000");
    private NumberFormat formatterForUSD = new DecimalFormat("#0.0000");
    private double ethValueInUSD;
    private double ethBalance;
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

        curenciesSpinner = findViewById(R.id.spinner_currency_select);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currencies, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        curenciesSpinner.setAdapter(adapter);
        curenciesSpinner.setOnItemSelectedListener(this);



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
                        new etherscanAPI().execute(createUserSpecificEtherscanURL());
                        new NanopoolAPI().execute(createUserSpecificNanopoolURL(userAdress));

                    }
                });
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask,10,30000); //todo maybe incorporate the period in a setting?
    }


    private URL createUserSpecificNanopoolURL(String accountAddress){
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

    private URL createUserSpecificEtherscanURL(){
        Uri uriBuilder = Uri.parse("https://api.etherscan.io/api?module=stats&action=ethprice&apikey=YourApiKeyToken").buildUpon()
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
                new NanopoolAPI().execute(createUserSpecificNanopoolURL(userAdress));
                break;
            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    public void setBalance(int i) {
        String valueTodisplay;

        if (i == 0) {
            valueTodisplay = String.valueOf(formatterForETH.format(ethBalance));
        } else {
            valueTodisplay = String.valueOf(formatterForUSD.format(ethBalance * ethValueInUSD));
        }

        balance.setText(valueTodisplay);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        setBalance(i);
        Toast.makeText(this,String.valueOf(i),Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

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
                    ethBalance = JSONdata.getDouble("balance");
                    setBalance(curenciesSpinner.getSelectedItemPosition());
                    //balance.setText(String.valueOf(formatter.format(ethBalance)));
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




    private class etherscanAPI extends AsyncTask<URL ,Void ,String> {

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
            JSONObject apiJSONobj;
            JSONObject JSONdata = null;


            try {
                apiJSONobj = new JSONObject(s);
                JSONdata = apiJSONobj.getJSONObject("result");

            } catch (JSONException e) {
                e.printStackTrace();
            }


                if (JSONdata != null) {
                    try {
                        ethValueInUSD = JSONdata.getDouble("ethusd");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

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
