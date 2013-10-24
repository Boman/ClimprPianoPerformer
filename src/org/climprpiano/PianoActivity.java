package org.climprpiano;

import java.util.ArrayList;
import java.util.List;

import jp.kshoji.driver.midi.activity.AbstractMultipleMidiActivity;
import jp.kshoji.driver.midi.device.MidiInputDevice;

import org.climprpiano.PianoManager.HandMode;
import org.climprpiano.PianoManager.PlayMode;
import org.climprpiano.PianoManager.PlayState;
import org.climprpiano.PianoManager.RepeatMode;
import org.climprpiano.midisheetmusic.ChooseSongActivity;
import org.climprpiano.midisheetmusic.FileUri;
import org.climprpiano.util.SystemUiHider;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * The main activity containing the control elements, their functionality and the setup of midi interface
 */
public class PianoActivity extends AbstractMultipleMidiActivity {

	public static final String MIDI_TITLE_ID = "MidiTitleID";

	int arr_images[] = { R.drawable.listen, R.drawable.play_pause, R.drawable.media_drum_kit, R.drawable.play }; // images for the playTypeSpinner

	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION; // the flags to pass to {@link SystemUiHider#getInstance}
	private SystemUiHider mSystemUiHider; // the instance of the {@link SystemUiHider} for this activity

	private PianoManager pianoManager;

	private PianoRollView pianoRollView;
	private PianoKeyboardView pianoKeyboardView;

	private Spinner playTypeSpinner;

	private ToggleButton leftHandCheckbox;
	private ToggleButton rigthHandCheckbox;

	public static final double SPEED_MULTIPLIER = 2;
	public static final int SEEK_BAR_DIVIDER = 20;
	private SeekBar speedBar;
	private TextView speedText;

	private Button playButton;
	private Button forwardButton;
	private Button backwardButton;

	private Spinner songSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.activity_piano);

		// set up the ActionBar
		final ActionBar actionBar = getActionBar();
		actionBar.setCustomView(R.layout.actionbar);
		actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(195, 0, 0, 0)));
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);

		// create the playTypeSpinner
		playTypeSpinner = (Spinner) findViewById(R.id.spinnerPlayType);
		playTypeSpinner.setAdapter(new PlayTypeAdapter(PianoActivity.this, android.R.layout.simple_spinner_item,
		        new String[] { "listen", "follow you", "rhythm", "play" }));

		// functionality of the hand checkboxes
		leftHandCheckbox = (ToggleButton) findViewById(R.id.checkBoxLeftHand);
		rigthHandCheckbox = (ToggleButton) findViewById(R.id.checkBoxRightHand);
		OnClickListener handsChangeListener = new View.OnClickListener() {
			public void onClick(View v) {
				if (leftHandCheckbox.isChecked() && rigthHandCheckbox.isChecked()) {
					pianoManager.setHandMode(HandMode.BOTH);
				} else if (leftHandCheckbox.isChecked() && !rigthHandCheckbox.isChecked()) {
					pianoManager.setHandMode(HandMode.LEFT);
				} else if (!leftHandCheckbox.isChecked() && rigthHandCheckbox.isChecked()) {
					pianoManager.setHandMode(HandMode.RIGHT);
				} else if (!leftHandCheckbox.isChecked() && !rigthHandCheckbox.isChecked()) {
					((ToggleButton) v).setChecked(true);
				}
			}
		};
		leftHandCheckbox.setOnClickListener(handsChangeListener);
		rigthHandCheckbox.setOnClickListener(handsChangeListener);

		// functionality of the speedBar
		speedBar = (SeekBar) findViewById(R.id.seekBarSpeed);
		speedBar.setMax(SEEK_BAR_DIVIDER);
		speedText = (TextView) findViewById(R.id.textViewSpeed);
		speedBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				double speed = 1.0 / SPEED_MULTIPLIER
				        * Math.pow(SPEED_MULTIPLIER * SPEED_MULTIPLIER, seekBar.getProgress() * 1.0 / SEEK_BAR_DIVIDER);
				pianoManager.setPlaySpeed(speed);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				double speed = 1.0 / SPEED_MULTIPLIER * Math.pow(SPEED_MULTIPLIER * SPEED_MULTIPLIER, progress * 1.0 / SEEK_BAR_DIVIDER);
				speedText.setText(PianoActivity.this.getResources().getString(R.string.speed) + ": "
				        + (int) (speed * 100) + "%");
			}
		});

		// functionality of the play, forward and backward button
		playButton = (Button) findViewById(R.id.buttonPlay);
		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mSystemUiHider.isVisible()) {
					mSystemUiHider.hide();
				}
				pianoManager.setPlayState(PlayState.PLAY);
			}
		});
		forwardButton = (Button) findViewById(R.id.buttonForward);
		forwardButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pianoManager.loopForward();
			}
		});
		backwardButton = (Button) findViewById(R.id.buttonBackward);
		backwardButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pianoManager.loopBackward();
			}
		});

		// create the songSpinner
		songSpinner = (Spinner) findViewById(R.id.spinnerSong);
		updateLatestSongs(null);
		songSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				FileUri item = (FileUri) parent.getItemAtPosition(pos);
				if (item == null) {
					songSpinner.setSelection(0);
					Intent intent = new Intent(PianoActivity.this, ChooseSongActivity.class);
					startActivity(intent);
				} else {
					pianoManager.setFileUri(item);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		// initialize the rollview
		pianoRollView = (PianoRollView) findViewById(R.id.piano_roll_container);
		Log.d("piano", pianoRollView.toString());
		pianoKeyboardView = (PianoKeyboardView) findViewById(R.id.piano_keyboard_container);
		pianoManager = new PianoManager(pianoRollView, pianoKeyboardView, this);
		pianoRollView.setPianoManager(pianoManager);

		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
			@Override
			public void onVisibilityChange(boolean visible) {
				if (visible) {
					pianoManager.setPlayState(PlayState.PAUSE);
					getActionBar().show();
				} else {
					getActionBar().hide();
				}
			}
		});

		// Parse the MidiFile from the raw bytes or load the last file
		Uri uri = this.getIntent().getData();
		String title = this.getIntent().getStringExtra(MIDI_TITLE_ID);
		FileUri file = null;
		if (uri == null) {
			List<FileUri> lastSongs = getLastSongs();
			if (lastSongs.size() >= 1) {
				file = lastSongs.get(0);
			} else {
				uri = Uri.parse("file:///android_asset/Beethoven__Moonlight_Sonata.mid");
				file = new FileUri(uri, title);
			}
		} else {
			file = new FileUri(uri, title);
		}
		pianoManager.setFileUri(file);
	}

	/** When the menu button is pressed, initialize the menus. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.piano_actions, menu);
		return true;
	}

	/**
	 * Callback when a menu item is selected. - Choose Song : Choose a new song - Song Settings : Adjust the sheet music and sound options - Save As Images:
	 * Save the sheet music as PNG images - Help : Display the HTML help screen
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			return true;
		case R.id.help:
			showHelp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Adapter for displaying the playType Spinner
	public class PlayTypeAdapter extends ArrayAdapter<String> {
		private LayoutInflater inflater;

		public PlayTypeAdapter(Context context, int textViewResourceId, String[] objects) {
			super(context, textViewResourceId, objects);
			inflater = LayoutInflater.from(context);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		public View getCustomView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.image_row_layout, null);
			}

			ImageView icon = (ImageView) convertView.findViewById(R.id.image);
			icon.setImageResource(arr_images[position]);

			return convertView;
		}
	}

	// Adapter for displaying the lastSongs Spinner
	public class SongAdapter<T> extends ArrayAdapter<T> {
		private LayoutInflater inflater;

		public SongAdapter(Context context, int resourceId, List<T> objects) {
			super(context, resourceId, objects);
			inflater = LayoutInflater.from(context);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent, android.R.layout.simple_spinner_item);
		}

		public View getCustomView(int position, View convertView, ViewGroup parent, int resourceId) {
			if (convertView == null) {
				convertView = inflater.inflate(resourceId, null);
			}

			TextView text = (TextView) convertView.findViewById(android.R.id.text1);
			FileUri file = (FileUri) this.getItem(position);
			if (file == null) {
				text.setText(PianoActivity.this.getResources().getString(R.string.more) + " ...");
			} else {
				text.setText(file.toString());
			}

			return convertView;
		}
	}

	/**
	 * @param playMode
	 *            the playMode to set
	 */
	public void setPlayMode(PlayMode playMode) {
		switch (playMode) {
		case LISTEN:
			playTypeSpinner.setSelection(0);
			break;
		case FOLLOW_YOU:
			playTypeSpinner.setSelection(1);
			break;
		case RYTHM_TAP:
			playTypeSpinner.setSelection(2);
			break;
		case PLAY_ALONG:
			playTypeSpinner.setSelection(3);
			break;
		default:
			break;
		}
	}

	/**
	 * @param handMode
	 *            the handMode to set
	 */
	public void setHandMode(HandMode handMode) {
		rigthHandCheckbox.setSelected(handMode == HandMode.BOTH || handMode == HandMode.LEFT);
		rigthHandCheckbox.setSelected(handMode == HandMode.BOTH || handMode == HandMode.RIGHT);
	}

	/**
	 * @param playSpeed
	 *            the playSpeed to set
	 */
	public void setPlaySpeed(double playSpeed) {
		speedBar.setProgress((int) (Math.log(playSpeed * SPEED_MULTIPLIER) / 2 / Math.log(SPEED_MULTIPLIER) * SEEK_BAR_DIVIDER));
	}

	/**
	 * @param repeatMode
	 *            the repeatMode to set
	 */
	public void setRepeatMode(RepeatMode repeatMode) {
		// TODO
	}

	private List<FileUri> getLastSongs() {
		ArrayList<FileUri> filelist = new ArrayList<FileUri>();
		SharedPreferences settings = getSharedPreferences("climprpiano.recentFiles", 0);
		String recentFilesString = settings.getString("recentFiles", null);
		if (recentFilesString == null) {
			return filelist;
		}
		try {
			JSONArray jsonArray = new JSONArray(recentFilesString);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject obj = jsonArray.getJSONObject(i);
				FileUri file = FileUri.fromJson(obj, this);
				if (file != null) {
					filelist.add(file);
				}
			}
		} catch (Exception e) {
		}
		return filelist;
	}

	public void updateLatestSongs(FileUri fileUri) {
		List<FileUri> lastSongs = getLastSongs();
		lastSongs = lastSongs.subList(0, Math.min(5, lastSongs.size()));
		if (fileUri != null && !lastSongs.contains(fileUri)) {
			lastSongs.add(0, fileUri);
		}
		lastSongs.add(null);
		ArrayAdapter<FileUri> songAdapter = new SongAdapter<FileUri>(this, android.R.layout.simple_spinner_item,
		        lastSongs);
		songAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		songSpinner.setAdapter(songAdapter);
	}

	/** Show the HTML help screen. */
	private void showHelp() {
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity(intent);
	}

	@Override
	public void onDeviceDetached(UsbDevice usbDevice) {

	}

	@Override
	public void onDeviceAttached(UsbDevice usbDevice) {

	}

	@Override
	public void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {

	}

	@Override
	public void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {

	}

	@Override
	public void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes) {
	}

	@Override
	public void onMidiSystemExclusive(MidiInputDevice sender, int cable, byte[] systemExclusive) {

	}

	@Override
	public void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		pianoManager.pianoKeyPress(note, -1);
	}

	@Override
	public void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		pianoManager.pianoKeyPress(note, velocity);
	}

	@Override
	public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int note, int pressure) {

	}

	@Override
	public void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value) {

	}

	@Override
	public void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program) {

	}

	@Override
	public void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int pressure) {

	}

	@Override
	public void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount) {

	}

	@Override
	public void onMidiSingleByte(MidiInputDevice sender, int cable, int byte1) {

	}
}
