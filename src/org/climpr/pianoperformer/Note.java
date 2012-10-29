package org.climpr.pianoperformer;

public class Note {
	public int tone;
	public int track;
	public int velocity;
	public int start;
	public int duration;

	public Note(int tone, int track, int velocity, int start, int duration) {
		super();
		this.tone = tone;
		this.track = track;
		this.velocity = velocity;
		this.start = start;
		this.duration = duration;
	}
}
