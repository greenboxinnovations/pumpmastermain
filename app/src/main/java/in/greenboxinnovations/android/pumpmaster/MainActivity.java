package in.greenboxinnovations.android.pumpmaster;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.vision.barcode.Barcode;

import junit.framework.Test;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    MyGlobals myGlobals;
    boolean isWiFiEnabled;
    CoordinatorLayout coordinatorLayout;
    private static final String APP_SHARED_PREFS = "prefs";
    private SharedPreferences sharedPrefs;
    private JSONObject jsonObject;
    private TextView petrol_rate, diesel_rate, user_name, pump_name, petrol_title, diesel_title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myGlobals = new MyGlobals(getApplicationContext());
        isWiFiEnabled = myGlobals.isWiFiEnabled();

        coordinatorLayout = findViewById(R.id.activity_main_layout);
        sharedPrefs = getApplicationContext().getSharedPreferences(APP_SHARED_PREFS, Context.MODE_PRIVATE);

        init();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scan = new Intent(getApplicationContext(), Scan.class);
                scan.putExtra("title","Scan Car");
                startActivityForResult(scan, 100);
            }
        });
    }

    private void init() {

        petrol_title = findViewById(R.id.tv_petrol_rate_title);
        diesel_title = findViewById(R.id.tv_diesel_rate_title);
        pump_name = findViewById(R.id.tv_pump_name);

        petrol_rate = findViewById(R.id.tv_petrol_rate);
        diesel_rate = findViewById(R.id.tv_diesel_rate);
        user_name = findViewById(R.id.tv_user_name);

        jsonObject = null;

        petrol_rate.setText(String.valueOf(sharedPrefs.getString("petrol_rate", "00.00")));
        diesel_rate.setText(String.valueOf(sharedPrefs.getString("diesel_rate", "00.00")));
        user_name.setText(sharedPrefs.getString("user_name", "error"));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (data != null) {
                final Barcode barcode = data.getParcelableExtra("barcode");
                String val = barcode.displayValue;
                Log.e("Car code", "" + val);
                Toast.makeText(getApplicationContext(), "Car Code is " + val, Toast.LENGTH_SHORT).show();
                isCodeValid(val);
            }
        }
        if (requestCode == 101 && resultCode == RESULT_OK) {
            if (data != null) {
                final Barcode barcode = data.getParcelableExtra("barcode");
                String val = barcode.displayValue;
                Log.e("Pump code", "" + val);
                Toast.makeText(getApplicationContext(), "Pump Code is " + val, Toast.LENGTH_SHORT).show();

                Intent i = new Intent(getApplicationContext(), NewTransaction.class);
                i.putExtra("jsonObject", jsonObject.toString());
                i.putExtra("pumpCode", val);
                startActivity(i);
            }
        }
    }


    // Ensure the right menu is setup
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Start your settings activity when a menu item is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

//        if (item.getItemId() == R.id.enterReceipt) {
//            showDialog();
//        }
        if (item.getItemId() == R.id.logout) {
            sharedPrefs.edit().clear().apply();
            Intent i = new Intent(getApplicationContext(), Login.class);
            startActivity(i);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialog() {
        final EditText input = new EditText(this);

        input.setWidth(60);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);


        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter receipt no")
//                .setMessage("What do you want to do next?")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String val = String.valueOf(input.getText());
                        Log.e("receipt no entered", val);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        dialog.show();

    }

    //local network check
    private void isCodeValid(final String val) {
        if (isWiFiEnabled) {

            String url = getResources().getString(R.string.url_main);

            url = url + "/exe/check_qr.php";

            Log.e("login response", url);

            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("qr", val);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    url, jsonObj,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            Log.e("login response", response.toString());
                            try {
                                if (response.getBoolean("success")) {

                                    Snackbar.make(coordinatorLayout, "Access Granted.", Snackbar.LENGTH_SHORT).show();

                                    jsonObject = response;
                                    Intent scan = new Intent(getApplicationContext(), Scan.class);
                                    scan.putExtra("title","Scan Pump");
                                    startActivityForResult(scan, 101);

                                } else {
                                    Log.e("result", "fail");
                                    Snackbar.make(coordinatorLayout, "Invalid Code", Snackbar.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Volley Error", "Error: " + error.getMessage());
                    Snackbar.make(coordinatorLayout, "Network Error", Snackbar.LENGTH_LONG).show();
                }
            }) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("charset", "utf-8");
                    return headers;
                }
            };
            MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);

        }
    }


}
