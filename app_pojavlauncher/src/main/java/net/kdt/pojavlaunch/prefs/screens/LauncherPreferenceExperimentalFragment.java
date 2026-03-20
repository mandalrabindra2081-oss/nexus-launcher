package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import com.nexuslauncher.app.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);
    }
}
