package org.labs.musaka.miningslavestats;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by rimogardino on 2/13/18.
 */

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }


}
