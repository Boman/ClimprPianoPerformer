package org.hexiano;

import java.util.HashMap;

import android.content.Context;
import android.media.SoundPool;

public class SoundController {

	Instrument mInstrument;
	HashMap<Integer, Integer> playedTones;

	public SoundController(Context con) {
		playedTones = new HashMap<Integer, Integer>();
		loadKeyboard(con);
	}

	protected void loadKeyboard(Context con) {
		// Context con = this.getApplicationContext();

		mInstrument = null;// (Instrument) getLastNonConfigurationInstance();
		if (mInstrument == null) {
			// If no retained audio, load it all up (slow).
			mInstrument = new PianoInstrument(con);
			// Redraw whenever a new note is ready.
			mInstrument.mSoundPool
					.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
						@Override
						public void onLoadComplete(SoundPool mSoundPool,
								int sampleId, int status) {
							if (mInstrument.sound_load_queue.hasNext()) {
								int[] tuple = mInstrument.sound_load_queue
										.next();
								mInstrument.addSound(tuple[0], tuple[1]);
							}
						}
					});
		}
	}

	public void play(int midiNoteNumber, int velocity) {
		playedTones.put(midiNoteNumber,
				mInstrument.play(midiNoteNumber, velocity));
	}

	public void stop(int midiNoteNumber) {
		if (playedTones.containsKey(midiNoteNumber)) {
			mInstrument.stop(playedTones.get(midiNoteNumber));
		}
	}

}
