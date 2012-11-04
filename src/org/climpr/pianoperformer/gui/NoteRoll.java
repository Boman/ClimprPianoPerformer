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
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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

	private double pulsesPerPixel;

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

		pulsesPerPixel = 5;

		NUM_KEYS = 88;
		NUM_WHITE_KEYS = 52;
		KEY_NOTE_OFFSET = 21;

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
	}

	/** Set the measured width and height */
	@Override
	protected void onMeasure(int widthspec, int heightspec) {
		int screenwidth = MeasureSpec.getSize(widthspec);
		int screenheight = MeasureSpec.getSize(heightspec);

		WhiteKeyWidth = (int) (screenwidth / NUM_WHITE_KEYS);
		BlackKeyWidth = WhiteKeyWidth / 2;
		BlackBorder = (screenwidth - WhiteKeyWidth * NUM_WHITE_KEYS) / 2;

		rollHeight = screenheight - WhiteKeyWidth * 5 - BlackBorder * 3;

		int width = BlackBorder * 2 + WhiteKeyWidth * NUM_WHITE_KEYS;
		int height = rollHeight;
		setMeasuredDimension(width, height);
		bufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		bufferCanvas = new Canvas(bufferBitmap);
		this.invalidate();
		draw();
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
		int y0 = rollHeight + (int) ((PianoActivity.piano.currentPulseTime - note.start - note.duration) / pulsesPerPixel);
		int y1 = rollHeight + (int) ((PianoActivity.piano.currentPulseTime - note.start) / pulsesPerPixel);
		if (inRange(y0, 0, rollHeight) || inRange(y1, 0, rollHeight)) {
			npd.setBounds((note.tone - KEY_NOTE_OFFSET) * WhiteKeyWidth, y0, (note.tone + 1 - KEY_NOTE_OFFSET) * WhiteKeyWidth, y1);
		}

		// Finally draw on the canvas
		npd.draw(bufferCanvas);
	}

	private boolean inRange(int a, int min, int max) {
		return a >= min && a <= max;
	}

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
