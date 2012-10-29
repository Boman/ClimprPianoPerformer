package org.climpr.pianoperformer;

/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

import org.climpr.pianoperformer.gui.NoteRoll;
import org.climpr.pianoperformer.gui.PianoKeyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * @class SheetMusicActivity
 * 
 *        The SheetMusicActivity is the main activity. The main components are:
 *        - MidiPlayer : The buttons and speed bar at the top. - Piano : For
 *        highlighting the piano notes during playback. - SheetMusic : For
 *        highlighting the sheet music notes during playback.
 * 
 */
public class PianoActivity extends Activity {
	public static final String MidiDataID = "MidiDataID";
	public static final String MidiTitleID = "MidiTitleID";

	public static Piano piano;
	private PianoKeyboard pianoKeyboard;
	private NoteRoll noteRoll;

	private LinearLayout layout;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		byte[] data = this.getIntent().getByteArrayExtra(MidiDataID);
		String title = this.getIntent().getStringExtra(MidiTitleID);
		piano = new Piano(data, title);

		createView();
	}

	/* Create the MidiPlayer and Piano views */
	void createView() {
		layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(layout);

		noteRoll = new NoteRoll(this);
		layout.addView(noteRoll);
		layout.requestLayout();

		pianoKeyboard = new PianoKeyboard(this);
		layout.addView(pianoKeyboard);
		layout.requestLayout();
	}

	/** Always display this activity in landscape mode. */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	/** When the menu button is pressed, initialize the menus. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// if (player != null) {
		// player.Pause();
		// }
		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.sheet_menu, menu);
		return true;
	}

	/**
	 * Callback when a menu item is selected. - Choose Song : Choose a new song
	 * - Song Settings : Adjust the sheet music and sound options - Save As
	 * Images: Save the sheet music as PNG images - Help : Display the HTML help
	 * screen
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// case R.id.choose_song:
		// chooseSong();
		// return true;
		// case R.id.song_settings:
		// changeSettings();
		// return true;
		// case R.id.save_images:
		// showSaveImagesDialog();
		// return true;
		// case R.id.help:
		// showHelp();
		// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * To change the sheet music options, start the SettingsActivity. Pass the
	 * current Midiptions as a parameter to the Intent. When the
	 * SettingsActivity has finished, the onActivityResult() method will be
	 * called.
	 */
	private void changeSettings() {
		// Intent intent = new Intent(this, SettingsActivity.class);
		// intent.putExtra(SettingsActivity.settingsID, options);
		// startActivityForResult(intent, settingsRequestCode);
	}

	/** Show the HTML help screen. */
	private void showHelp() {
		// Intent intent = new Intent(this, HelpActivity.class);
		// startActivity(intent);
	}

	/**
	 * This is the callback when the SettingsActivity is finished. Get the
	 * modified MidiOptions (passed as a parameter in the Intent). Re-create the
	 * SheetMusic View with the new options.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// if (requestCode != settingsRequestCode) {
		// return;
		// }
		// options = (MidiOptions)
		// intent.getSerializableExtra(SettingsActivity.settingsID);
		//
		// // Check whether the default instruments have changed.
		// for (int i = 0; i < options.instruments.length; i++) {
		// if (options.instruments[i] !=
		// midifile.getTracks().get(i).getInstrument()) {
		// options.useDefaultInstruments = false;
		// }
		// }
		// SharedPreferences settings = getPreferences(0);
		// SharedPreferences.Editor editor = settings.edit();
		// editor.putBoolean("scrollVert", options.scrollVert);
		// editor.commit();
		// createSheetMusic(options);
	}

	Thread t = null;

	/** When this activity resumes, redraw all the views */
	@Override
	protected void onResume() {
		super.onResume();
		layout.requestLayout();
		pianoKeyboard.invalidate();
		layout.requestLayout();

		if (t == null) {
			t = new Thread(new Runnable() {
				@Override
				public void run() {
					int i = 40;
					while (true) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						pianoKeyboard.shadeKey(i, 0);
						if (i++ > 60) {
							i = 40;
						}
						pianoKeyboard.shadeKey(i, 1);
					}
				}
			});
			// t.run();
		}

		Intent intent = getIntent();
		String action = intent.getAction();

		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			Toast.makeText(getApplicationContext(), device.getVendorId() + " attached", Toast.LENGTH_SHORT).show();
			// setDevice(device);
		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			Toast.makeText(getApplicationContext(), device.getVendorId() + " detached", Toast.LENGTH_SHORT).show();
			// if (mDevice != null && mDevice.equals(device)) {
			// setDevice(null);
			// }
		}
	}

	/** When this activity pauses, stop the music */
	@Override
	protected void onPause() {
		// if (player != null) {
		// player.Pause();
		// }
		super.onPause();
	}
}
