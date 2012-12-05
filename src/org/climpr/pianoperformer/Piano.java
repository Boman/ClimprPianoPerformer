package org.climpr.pianoperformer;

import java.util.List;
import java.util.Vector;

import org.climpr.pianoperformer.midi.MidiFile;
import org.climpr.pianoperformer.midi.MidiNote;
import org.climpr.pianoperformer.midi.MidiTrack;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class Piano extends MidiFile {
	private List<Note> notes;

	/** Timer used to update the sheet music while playing */
	Handler timer;

	double speed;
	public double currentPulseTime;
	public double lastPulse;

	private double pulsesPerMsec;

	public Piano(byte[] rawdata, String filename) {
		super(rawdata, filename);

		speed = 1;
		currentPulseTime = 0;
		lastPulse = 0;

		pulsesPerMsec = getTime().getQuarter() / 1000.0;
		Log.d("piano", "pulsesPerMsec " + pulsesPerMsec);

		notes = new Vector<Note>();
		for (MidiTrack track : tracks) {
			for (MidiNote note : track.getNotes()) {
				notes.add(new Note(note.getNumber(), note.getChannel(), 128, note.getStartTime(), note.getDuration()));
				lastPulse = Math.max(lastPulse, note.getStartTime() + note.getDuration());
			}
		}
	}

	public List<Note> getNotes() {
		return notes;
	}

	public void screenKeyPressed(int note) {

	}

	public void screenKeyReleased(int note) {

	}

	public void midiKeyPressed(int note) {

	}

	public void midiKeyReleased(int note) {

	}
}
