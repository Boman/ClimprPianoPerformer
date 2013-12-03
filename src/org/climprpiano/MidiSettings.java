package org.climprpiano;

import java.util.List;

import org.climprpiano.PianoManager.PlayMode;

import android.content.Context;
import android.content.SharedPreferences;

import com.midisheetmusic.FileUri;

/**
 * @class MidiSettings The MidiSettings class contains the available options for modifying the sheet music and sound. These options are collected from the
 *        SettingsActivity, and are passed to the SheetMusic and MidiPlayer classes.
 */
public class MidiSettings {

	public boolean showPiano; // Display the piano
	public List<PlayMode> trackPlayModes; // Which tracks to display (true = display)
	public int transpose; // Shift note key up/down by given amount
	public int combineInterval; // Combine notes within given time interval (msec)

	public List<Double> loopMarks; // the positions where the song can be repeated
	public double playSpeed;

	public MidiSettings() {
	}

	public static MidiSettings getSettings(Context context, FileUri midiFile) {
		MidiSettings settings = new MidiSettings();
		SharedPreferences prefs = context.getSharedPreferences("climprpiano.midiSettings_" + midiFile.toString(),
				Context.MODE_PRIVATE);
		return settings;
	}

	public static void setSettings(Context context, FileUri midiFile) {
		SharedPreferences prefs = context.getSharedPreferences("climprpiano.midiSettings_" + midiFile.toString(), 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.commit();
	}
}
