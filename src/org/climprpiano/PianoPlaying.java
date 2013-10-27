package org.climprpiano;

import java.util.Vector;

import org.climprpiano.midisheetmusic.MidiNote;

public class PianoPlaying {
	Vector<MidiNote> playedNotes = new Vector<MidiNote>();

	public void newNote(int midiNote, int velocity, int time) {
		if (velocity > 0) {
			playedNotes.add(new MidiNote(time, 0, midiNote, 0));
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
				lastNumberNote.NoteOff(time);
			}
		}
	}
}
