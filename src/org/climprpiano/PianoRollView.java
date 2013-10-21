package org.climprpiano;

import java.util.Collections;
import java.util.List;

import org.climprpiano.midisheetmusic.MidiNote;
import org.climprpiano.midisheetmusic.MidiTrack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;

public class PianoRollView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {
	private PianoManager pianoManager;

	private Paint paint;
	/** The paint options for drawing */
	private boolean surfaceReady;
	/** True if we can draw on the surface */

	private int width; // the width of the view
	private int height;// the height of the view

	private int keyOffset; // the most left keys midi note
	private int numKeys; // the number of keys
	private int[][] keyPositions; // an array containing for each key if it is white the y1 and y2 position

	private double pixelsPerPulse;

	private NinePatchDrawable[] noteBars;

	private int startMotionY;
	/** The y pixel when a touch motion starts */
	private float deltaY;
	/** The change in y-pixel of the last motion */

	private int numTouches = 0; // number of finger touches on the screen
	private float startMotionCenter;
	private float startMotionScale;

	private long lastMotionTime;
	/** Time of the last motion event (millsec) */
	private Handler scrollTimer;
	/** Timer for doing 'fling' scrolling */

	// variables for detecting long toucghes
	private int firstMotionY;
	private Handler touchTimer;

	public PianoRollView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setAntiAlias(false);
		paint.setTextSize(9.0f);

		pixelsPerPulse = 0.15;

		noteBars = new NinePatchDrawable[] {
		        (NinePatchDrawable) getContext().getResources().getDrawable(R.drawable.note_bar_orange),
		        (NinePatchDrawable) getContext().getResources().getDrawable(R.drawable.note_bar_blue),
		        (NinePatchDrawable) getContext().getResources().getDrawable(R.drawable.note_bar_green),
		        (NinePatchDrawable) getContext().getResources().getDrawable(R.drawable.note_bar_pink) };

		scrollTimer = new Handler();
		touchTimer = new Handler();

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		setOnTouchListener(this);

		setDisplayedKeys(21, 108);
	}

	/** Set the measured width and height */
	@Override
	protected void onMeasure(int widthspec, int heightspec) {
		width = MeasureSpec.getSize(widthspec);
		height = MeasureSpec.getSize(heightspec);

		keyPositions = PianoKeyboardView.calculateKeyPositions(width, keyOffset, numKeys);

		setMeasuredDimension(width, height);
		this.invalidate();
		draw();
	}

	// sets the shown keys keyboard by a start and end midi note
	public void setDisplayedKeys(int startMidiNote, int endMidiNote) {
		// check if first note is a black key => always start with a white key
		if (!PianoKeyboardView.IS_WHITE_KEY[startMidiNote % 12]) {
			startMidiNote--;
		}

		// check if last note is a black key => always end with a white key
		if (!PianoKeyboardView.IS_WHITE_KEY[(endMidiNote) % 12]) {
			endMidiNote++;
		}

		keyOffset = startMidiNote;
		numKeys = endMidiNote - startMidiNote + 1;

		keyPositions = PianoKeyboardView.calculateKeyPositions(width, keyOffset, numKeys);
	}

	public void update() {
		draw();
	}

	/** Obtain the drawing canvas and call onDraw() */
	private void draw() {
		if (!surfaceReady) {
			return;
		}
		SurfaceHolder holder = getHolder();
		Canvas canvas = holder.lockCanvas();
		if (canvas == null) {
			return;
		}
		doDraw(canvas);
		holder.unlockCanvasAndPost(canvas);
	}

	/** Draw the Roll. */
	protected void doDraw(Canvas canvas) {
		if (!surfaceReady || keyPositions == null) {
			return;
		}

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.rgb(35, 35, 35));
		canvas.drawRect(0, 0, width, height, paint);

		// draw vertical grey lines at the position of each black key
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.rgb(20, 20, 20));
		for (int i = 1; i < keyPositions.length; ++i) {
			int[] key = keyPositions[i];
			if (key[0] == PianoKeyboardView.WHITE_KEY && keyPositions[i - 1][0] == PianoKeyboardView.BLACK_KEY) {
				canvas.drawLine(key[1], 0, key[1], height, paint);
			}
		}

		// draw horizontal grey lines at the position of each measure
		paint.setStyle(Paint.Style.STROKE);
		for (double i = (height + pianoManager.getCurrentPulseTime() * pixelsPerPulse)
		        % (pianoManager.getMidifile().getTime().getMeasure() * pixelsPerPulse); i < height; i += pianoManager
		        .getMidifile().getTime().getMeasure()
		        * pixelsPerPulse) {
			paint.setColor(Color.rgb(120, 120, 120));
			canvas.drawLine(0, (int) i, width, (int) i, paint);
			paint.setColor(Color.rgb(140, 140, 140));
			canvas.drawLine(0, (int) i + 1, width, (int) i + 1, paint);
		}

		// draw horizontal red lines at the position of each loop mark
		paint.setStyle(Paint.Style.STROKE);
		for (double loopMark : pianoManager.getLoopMarks()) {
			loopMark = height - (loopMark - pianoManager.getCurrentPulseTime()) * pixelsPerPulse;
			if (loopMark > 0 && loopMark < height) {
				paint.setColor(Color.rgb(150, 0, 0));
				canvas.drawLine(0, (int) loopMark, width, (int) loopMark, paint);
				paint.setColor(Color.rgb(170, 20, 20));
				canvas.drawLine(0, (int) loopMark + 1, width, (int) loopMark + 1, paint);
			}
		}

		// draw the note bars
		for (MidiTrack track : pianoManager.getMidifile().getTracks()) {
			for (MidiNote note : track.getNotes()) {
				drawNote(note, track.trackNumber(), canvas);
			}
		}
	}

	private void drawNote(MidiNote note, int track, Canvas canvas) {
		// calculate top and bottom position
		int y0 = height
		        - (int) ((note.getStartTime() + note.getDuration() - pianoManager.getCurrentPulseTime()) * pixelsPerPulse);
		int y1 = height - (int) ((note.getStartTime() - pianoManager.getCurrentPulseTime()) * pixelsPerPulse);
		// check if the note bar is within the canvas
		if (y0 <= height && y1 >= 0 && track < noteBars.length) {
			// set bounds of note bar
			noteBars[track].setBounds(keyPositions[note.getNumber() - keyOffset][1], y0, keyPositions[note.getNumber()
			        - keyOffset][2], y1);
			// draw on the canvas
			noteBars[track].draw(canvas);
		}
	}

	// check for long press and add or remove loop mark
	Runnable TouchTimer = new Runnable() {
		@Override
		public void run() {
			if (numTouches == 1 && Math.abs(firstMotionY - startMotionY) <= 10) {
				List<Double> loopMarks = pianoManager.getLoopMarks();
				Collections.sort(loopMarks);
				for (Double loopMark : loopMarks) {
					if (Math.abs(height - startMotionY + (pianoManager.getCurrentPulseTime() - loopMark)
					        * pixelsPerPulse) < pianoManager.getMidifile().getTime().getMeasure() * pixelsPerPulse / 6) {
						pianoManager.removeLoopMark(loopMark);
						return;
					}
				}
				for (double i = (height + pianoManager.getCurrentPulseTime() * pixelsPerPulse)
				        % (pianoManager.getMidifile().getTime().getMeasure() * pixelsPerPulse); i < height; i += pianoManager
				        .getMidifile().getTime().getMeasure()
				        * pixelsPerPulse) {
					if (Math.abs(startMotionY - i) < pianoManager.getMidifile().getTime().getMeasure() * pixelsPerPulse
					        / 6) {
						pianoManager.addLoopMark((height - i) / pixelsPerPulse + pianoManager.getCurrentPulseTime());
						return;
					}
				}
				pianoManager.addLoopMark((height - startMotionY) / pixelsPerPulse + pianoManager.getCurrentPulseTime());
			}
		}
	};

	/**
	 * Handle touch/motion events to implement scrolling the sheet music. - On down touch, store the (x,y) of the touch - On a motion event, calculate the delta
	 * (change) in x, y. Update the scrolX, scrollY and redraw the sheet music. - On a up touch, implement a 'fling'. Call flingScroll every 50 msec for the
	 * next 2 seconds.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			numTouches = 1;
			deltaY = 0;
			scrollTimer.removeCallbacks(flingScroll);
			startMotionY = (int) event.getY();
			firstMotionY = startMotionY;
			touchTimer.postDelayed(TouchTimer, 1000);
			return true;
		case MotionEvent.ACTION_POINTER_DOWN:
			numTouches = 2;
			touchTimer.removeCallbacks(TouchTimer);
			startMotionCenter = (event.getY() + event.getY(1)) / 2;
			startMotionScale = spacing(event);
			return true;
		case MotionEvent.ACTION_MOVE:
			if (numTouches == 1) {
				lastMotionTime = AnimationUtils.currentAnimationTimeMillis();
				deltaY = startMotionY - event.getY();
				startMotionY = (int) event.getY();
				pianoManager.setCurrentPulseTime(pianoManager.getCurrentPulseTime() - deltaY / pixelsPerPulse);
			} else if (numTouches == 2) {
				double middlePulseTime = pianoManager.getCurrentPulseTime() + (height - startMotionCenter)
				        / pixelsPerPulse;
				pixelsPerPulse *= spacing(event) / startMotionScale;
				pianoManager.setCurrentPulseTime((middlePulseTime * pixelsPerPulse - (height - startMotionCenter))
				        / pixelsPerPulse);
				startMotionCenter = (event.getY() + event.getY(1)) / 2;
				startMotionScale = spacing(event);
			}
			return true;
		case MotionEvent.ACTION_POINTER_UP:
			numTouches = 1;
			startMotionY = (int) event.getY();
			deltaY = 0;
			return true;
		case MotionEvent.ACTION_UP:
			numTouches = 0;
			long deltaTime = AnimationUtils.currentAnimationTimeMillis() - lastMotionTime;

			if (deltaTime >= 100) {
				return true;
			}

			/*
			 * Keep scrolling for 2 more seconds. Scale the delta to 20 msec. Make sure delta doesn't exceed the maximum scroll delta.
			 */
			int msecInterval = 20;
			deltaY = deltaY * msecInterval / deltaTime;
			int maxscroll = height / 4;
			if (Math.abs(deltaY) > maxscroll) {
				deltaY = deltaY * maxscroll / Math.abs(deltaY);
			}
			int duration = 2000;
			for (int i = msecInterval; i <= duration; i += msecInterval) {
				scrollTimer.postDelayed(flingScroll, i);
			}
			return true;
		default:
			return false;
		}
	}

	/** Determine the space between the first two fingers */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * The timer callback for doing 'fling' scrolling. Adjust the scrollX/scrollY using the last delta. Redraw the sheet music. Then, schedule this timer again,
	 * after 30 msec.
	 */
	Runnable flingScroll = new Runnable() {
		public void run() {
			if (Math.abs(deltaY) >= 5) {
				pianoManager.setCurrentPulseTime(pianoManager.getCurrentPulseTime() - deltaY / pixelsPerPulse);
				deltaY = deltaY * 9.2f / 10.0f;
			}
		}
	};

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		draw();
	}

	/** Surface is ready for shading the notes */
	public void surfaceCreated(SurfaceHolder holder) {
		surfaceReady = true;
		draw();
	}

	/** Surface has been destroyed */
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceReady = false;
	}

	/**
	 * @param pianoManager
	 *            the pianoManager to set
	 */
	public void setPianoManager(PianoManager pianoManager) {
		this.pianoManager = pianoManager;
	}
}
