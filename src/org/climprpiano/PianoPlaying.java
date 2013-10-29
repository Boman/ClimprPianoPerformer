package org.climprpiano;

import java.util.ArrayList;
import java.util.Vector;

import org.climprpiano.midisheetmusic.MidiNote;

import android.util.Log;

public class PianoPlaying {

	private PianoManager pianoManager;

	private Vector<MidiNote> playedNotes; // the notes which were played with the start and end exactly to the piano player

	private double startPulse; // the time of the song when the playing started

	private Vector<MidiNote> correctNotes; // notes which were played correctly by the player

	public PianoPlaying(PianoManager pianoManager, double currentPulseTime) {
		this.pianoManager = pianoManager;
		playedNotes = new Vector<MidiNote>();
		startPulse = currentPulseTime;
		correctNotes = new Vector<MidiNote>();
	}

	public void newNote(int midiNote, int velocity, int pulseTime) {
		if (velocity > 0) {
			playedNotes.add(new MidiNote(pulseTime, 0, midiNote, 0));
		} else if (velocity == 0) {
			MidiNote lastNumberNote = null;
			for (MidiNote playedNote : playedNotes) {
				// look for played notes on the given channel
				if (playedNote.getNumber() == midiNote) {
					// look if it is the most recent played note
					if (lastNumberNote == null || playedNote.getStartTime() > lastNumberNote.getStartTime()) {
						lastNumberNote = playedNote;
					}
				}
			}
			if (lastNumberNote != null) {
				lastNumberNote.NoteOff(pulseTime);
			}
		}
	}

	public boolean wasNotePlayed(MidiNote midiNote) {
		return correctNotes.contains(midiNote);
	}

	public void correctNotePlayed(MidiNote note) {
		correctNotes.add(note);
	}
}
