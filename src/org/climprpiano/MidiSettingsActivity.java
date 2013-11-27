package org.climprpiano;

import android.app.Activity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MidiSettingsActivity extends Activity {

	public static final double SPEED_MULTIPLIER = 2; // the range of maximum speed manipulation (2 => 50%-200%)
	public static final int SEEK_BAR_DIVIDER = 20; // the number of steps of the speed bar
	private SeekBar speedBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_midi_settings);

		// functionality of the speedBar
		speedBar = (SeekBar) findViewById(R.id.seekBarSpeed);
		speedBar.setMax(SEEK_BAR_DIVIDER);
		speedBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				double speed = 1.0 / SPEED_MULTIPLIER
						* Math.pow(SPEED_MULTIPLIER * SPEED_MULTIPLIER, seekBar.getProgress() * 1.0 / SEEK_BAR_DIVIDER);
				// TODO: pianoManager.setPlaySpeed(speed);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				double speed = 1.0 / SPEED_MULTIPLIER
						* Math.pow(SPEED_MULTIPLIER * SPEED_MULTIPLIER, progress * 1.0 / SEEK_BAR_DIVIDER);
				// TODO: speedText.setText(PianoActivity.this.getResources().getString(R.string.speed) + ": " + (int) (speed * 100) + "%");
			}
		});
	}
}
