package org.climpr.pianoperformer;

import java.util.List;
import java.util.Vector;

import org.climpr.pianoperformer.midi.MidiFile;
import org.climpr.pianoperformer.midi.MidiNote;
import org.climpr.pianoperformer.midi.MidiTrack;

public class Piano extends MidiFile {
	private List<Note> notes;

	public Piano(byte[] rawdata, String filename) {
		super(rawdata, filename);
	}

	public List<Note> getNotes() {
		if (notes == null) {
			notes = new Vector<Note>();
			for (MidiTrack track : tracks) {
				for (MidiNote note : track.getNotes()) {
					notes.add(new Note(note.getNumber(), note.getChannel(), 128, note.getStartTime(), note.getDuration()));
				}
			}
		}
		return notes;
	}
}
