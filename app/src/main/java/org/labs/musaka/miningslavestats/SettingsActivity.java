package org.labs.musaka.miningslavestats;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by rimogardino on 2/13/18.
 */

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }


}
