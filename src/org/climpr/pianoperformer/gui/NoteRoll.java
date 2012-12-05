package org.climpr.pianoperformer.gui;

import org.climpr.pianoperformer.Note;
import org.climpr.pianoperformer.PianoActivity;
import org.climpr.pianoperformer.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AnimationUtils;

public class NoteRoll extends SurfaceView implements SurfaceHolder.Callback {
	private static int WhiteKeyWidth;
	private static int BlackKeyWidth;
	private static int BlackBorder;

	private Paint paint;
	/** The paint options for drawing */
	private boolean surfaceReady;
	/** True if we can draw on the surface */
	private Bitmap bufferBitmap;
	/** The bitmap for double-buffering */
	private Canvas bufferCanvas;

	private int gray1, gray2, gray3, shade1, shade2;

	private int rollHeight;

	private final int NUM_KEYS;
	private final int NUM_WHITE_KEYS;
	private final int KEY_NOTE_OFFSET;

	private int[] keyPositions;

	private double pixelsPerPulse;

	private int scrollY;
	private int startMotionY;
	/** The y pixel when a touch motion starts */
	private float deltaY;
	/** The change in y-pixel of the last motion */
	private boolean inMotion;
	/** True if we're in a motion event */
	private long lastMotionTime;
	/** Time of the last motion event (millsec) */
	private Handler scrollTimer;

	/** Timer for doing 'fling' scrolling */

	public NoteRoll(Context context) {
		super(context);

		WhiteKeyWidth = 0;
		paint = new Paint();
		paint.setAntiAlias(false);
		paint.setTextSize(9.0f);

		gray1 = Color.rgb(16, 16, 16);
		gray2 = Color.rgb(90, 90, 90);
		gray3 = Color.rgb(200, 200, 200);
		shade1 = Color.rgb(210, 205, 220);
		shade2 = Color.rgb(150, 200, 220);

		pixelsPerPulse = 0.2;

		NUM_KEYS = 88;
		NUM_WHITE_KEYS = 52;
		KEY_NOTE_OFFSET = 21;

		keyPositions = new int[NUM_KEYS];
		
        scrollTimer = new Handler();

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
	}

	public NoteRoll(Context context, AttributeSet attrs) {
		this(context);
	}

	/** Set the measured width and height */
	@Override
	protected void onMeasure(int widthspec, int heightspec) {
		int screenwidth = MeasureSpec.getSize(widthspec);
		int screenheight = MeasureSpec.getSize(heightspec);

		WhiteKeyWidth = (int) (screenwidth / NUM_WHITE_KEYS);
		BlackKeyWidth = WhiteKeyWidth / 2;
		BlackBorder = (screenwidth - WhiteKeyWidth * NUM_WHITE_KEYS) / 2;

		calcKeyPositions();

		rollHeight = screenheight;

		int width = BlackBorder * 2 + WhiteKeyWidth * NUM_WHITE_KEYS;
		int height = rollHeight;
		setMeasuredDimension(width, height);
		Log.d("roll", width + " " + height);
		bufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bufferCanvas = new Canvas(bufferBitmap);
		this.invalidate();
		draw();
	}

	private void calcKeyPositions() {
		int note = KEY_NOTE_OFFSET % 12; // zero means C
		int pos = 0;
		for (int i = 0; i < NUM_KEYS; i++) {
			double posOffset = 0;//((octave - octaveStart) * 7 - noteStart + (noteStart > 4 ? (noteStart > 11 ? 2 : 1) : 0)) * WhiteKeyWidth;
			int posAdd = 0;
			switch (note) {
			case 0:
				posAdd = 1;
				break;
			case 1:
				posOffset = -0.6f * BlackKeyWidth;
				break;
			case 2:
				posAdd = 1;
				break;
			case 3:
				posOffset = -0.4f * BlackKeyWidth;
				break;
			case 4:
				posAdd = 1;
				break;
			case 5:
				posAdd = 1;
				break;
			case 6:
				posOffset = -0.6f * BlackKeyWidth;
				break;
			case 7:
				posAdd = 1;
				break;
			case 8:
				posOffset = -0.5f * BlackKeyWidth;
				break;
			case 9:
				posAdd = 1;
				break;
			case 10:
				posOffset = -0.4f * BlackKeyWidth;
				break;
			case 11:
				posAdd = 1;
				break;
			}
			keyPositions[i] = pos + (int) (posOffset);
			pos += posAdd * WhiteKeyWidth;
			note++;
			if (note >= 12) {
				note = 0;
			}
		}
	}

	public void update() {
		draw();
	}

	/** Obtain the drawing canvas and call onDraw() */
	void draw() {
		if (!surfaceReady) {
			return;
		}
		SurfaceHolder holder = getHolder();
		Canvas canvas = holder.lockCanvas();
		if (canvas == null) {
			return;
		}
		onDraw(canvas);
		holder.unlockCanvasAndPost(canvas);
	}

	/** Draw the Piano. */
	@Override
	protected void onDraw(Canvas canvas) {
		if (!surfaceReady || bufferBitmap == null || WhiteKeyWidth == 0) {
			return;
		}

		paint.setAntiAlias(false);
		bufferCanvas.translate(BlackBorder, 0);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.rgb(55, 55, 55));
		bufferCanvas.drawRect(0, 0, WhiteKeyWidth * NUM_WHITE_KEYS, rollHeight, paint);
		paint.setStyle(Paint.Style.STROKE);

		for (int i = 0; i < NUM_WHITE_KEYS; i++) {
			int left = i * WhiteKeyWidth;
			// draw white key separation
			if (i > 0) {
				paint.setColor(gray2);
				bufferCanvas.drawLine(left, 0, left, rollHeight, paint);
			}
		}

		for (Note note : PianoActivity.piano.getNotes()) {
			drawNote(note);
		}

		bufferCanvas.translate(-(BlackBorder), 0);

		canvas.drawBitmap(bufferBitmap, 0, 0, paint);
	}

	private void drawNote(Note note) {
		// Load the image as a NinePatch drawable
		NinePatchDrawable npd = (NinePatchDrawable) getContext().getResources().getDrawable(R.drawable.note_bar_green);

		// Set its bound where you need
		int y0 = rollHeight
				+ (int) ((PianoActivity.piano.currentPulseTime - note.start - note.duration) * pixelsPerPulse);
		int y1 = rollHeight + (int) ((PianoActivity.piano.currentPulseTime - note.start) * pixelsPerPulse);
		if (inRange(y0, 0, rollHeight) || inRange(y1, 0, rollHeight)) {
			npd.setBounds(keyPositions[note.tone - KEY_NOTE_OFFSET], y0, keyPositions[note.tone - KEY_NOTE_OFFSET]
					+ (keyPositions[note.tone - KEY_NOTE_OFFSET] % WhiteKeyWidth == 0 ? WhiteKeyWidth : BlackKeyWidth),
					y1);
		}

		// Finally draw on the canvas
		npd.draw(bufferCanvas);
	}

	private boolean inRange(int a, int min, int max) {
		return a >= min && a <= max;
	}

	/**
	 * Check that the scrollX/scrollY position does not exceed the bounds of the
	 * sheet music.
	 */
	private void checkScrollBounds() {
		// Get the width/height of the scrollable area
		//		int scrollheight = (int) (sheetheight * zoom);
		//
		//		if (scrollY < 0) {
		//			scrollY = 0;
		//		}
		//		if (scrollY > scrollheight - viewheight / 2) {
		//			scrollY = scrollheight - viewheight / 2;
		//		}
	}

	/**
	 * Handle touch/motion events to implement scrolling the sheet music. - On
	 * down touch, store the (x,y) of the touch - On a motion event, calculate
	 * the delta (change) in x, y. Update the scrolX, scrollY and redraw the
	 * sheet music. - On a up touch, implement a 'fling'. Call flingScroll every
	 * 50 msec for the next 2 seconds.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			deltaY = 0;
			scrollTimer.removeCallbacks(flingScroll);
			//			if (player != null && player.getVisibility() == View.GONE) {
			//				player.Pause();
			//				inMotion = false;
			//				return true;
			//			}
			inMotion = true;
			startMotionY = (int) event.getY();
			return true;
		case MotionEvent.ACTION_MOVE:
			if (!inMotion)
				return false;

			deltaY = startMotionY - event.getY();
			startMotionY = (int) event.getY();
			scrollY += (int) deltaY;
			PianoActivity.piano.currentPulseTime -= deltaY / pixelsPerPulse;

			checkScrollBounds();
			lastMotionTime = AnimationUtils.currentAnimationTimeMillis();
			draw();
			return true;

		case MotionEvent.ACTION_UP:
			inMotion = false;
			long deltaTime = AnimationUtils.currentAnimationTimeMillis() - lastMotionTime;
			if (deltaTime >= 100) {
				return true;
			}
			if (Math.abs(deltaY) <= 5) {
				return true;
			}

			/*
			 * Keep scrolling for 2 more seconds. Scale the delta to 20 msec.
			 * Make sure delta doesn't exceed the maximum scroll delta.
			 */
			int msecInterval = 20;
			deltaY = deltaY * msecInterval / deltaTime;
			//			int maxscroll = StaffHeight * 4;
			//			if (Math.abs(deltaY) > maxscroll) {
			//				deltaY = deltaY / Math.abs(deltaY) * StaffHeight;
			//			}
			int duration = 2000 / msecInterval;
			for (int i = 1; i <= duration; i++) {
				scrollTimer.postDelayed(flingScroll, i * msecInterval);
			}
			return true;
		default:
			return false;
		}
	}

	/**
	 * The timer callback for doing 'fling' scrolling. Adjust the
	 * scrollX/scrollY using the last delta. Redraw the sheet music. Then,
	 * schedule this timer again, after 30 msec.
	 */
	Runnable flingScroll = new Runnable() {
		public void run() {
			if (Math.abs(deltaY) >= 5) {
				scrollY += (int) deltaY;
				PianoActivity.piano.currentPulseTime -= deltaY / pixelsPerPulse;
				checkScrollBounds();
				draw();
				deltaY = deltaY * 9.2f / 10.0f;
			}
		}
	};

	/** TODO ?? */
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
}
