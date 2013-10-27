package org.climprpiano;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SongSettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.song_settings);
	}
}