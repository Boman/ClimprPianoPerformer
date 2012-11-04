package org.climpr.pianoperformer.gui;

/*
 * Copyright (c) 2009-2012 Madhav Vaidyanathan, Falko Thomale
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @class Piano
 * 
 *        The Piano Control is the panel at the top that displays the piano, and
 *        highlights the piano notes during playback. The main methods are:
 * 
 *        ShadeNotes() - Shade notes on the piano that occur at a given pulse
 *        time.
 * 
 */
public class PianoKeyboard extends SurfaceView implements SurfaceHolder.Callback {
	private static int WhiteKeyWidth;
	/** Width of a single white key */
	private static int WhiteKeyHeight;
	/** Height of a single white key */
	private static int BlackKeyWidth;
	/** Width of a single black key */
	private static int BlackKeyHeight;
	/** Height of a single black key */
	private static int borderLR, borderT, borderB;
	/** The width of the black border around the keys */

	/* The colors for drawing black/gray lines */
	private int gray1, gray2, gray3;
	private int[] shade;

	/** Display the letter for each piano note */
	private Paint paint;
	/** The paint options for drawing */
	private boolean surfaceReady;
	/** True if we can draw on the surface */
	private Bitmap bufferBitmap;
	/** The bitmap for double-buffering */
	private Canvas bufferCanvas;
	/** The canvas for double-buffering */

	private final int NUM_KEYS;
	private final int NUM_WHITE_KEYS;
	private final int KEY_NOTE_OFFSET;

	private int[] keyPositions;

	/*
	 * Shade the given note with the given brush. We only draw notes from
	 * notenumber 24 to 96. (Middle-C is 60).
	 */
	private int[] shadedKeys;

	HashMap<Integer, PointF> touchPoints = new HashMap<Integer, PointF>();
	private boolean[] touchedNotes;

	/** Create a new Piano. */
	public PianoKeyboard(Context context) {
		super(context);
		WhiteKeyWidth = 0;
		paint = new Paint();
		paint.setAntiAlias(false);
		paint.setTextSize(9.0f);
		gray1 = Color.rgb(16, 16, 16);
		gray2 = Color.rgb(90, 90, 90);
		gray3 = Color.rgb(200, 200, 200);
		shade = new int[2];
		shade[0] = Color.rgb(0, 255, 0);
		shade[1] = Color.rgb(0, 0, 255);

		NUM_KEYS = 88;
		NUM_WHITE_KEYS = 52;
		KEY_NOTE_OFFSET = 21;

		keyPositions = new int[NUM_KEYS];
		shadedKeys = new int[NUM_KEYS];
		touchedNotes = new boolean[NUM_KEYS];

		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
	}

	public PianoKeyboard(Context context, AttributeSet attrs) {
		this(context);
	}

	/** Set the measured width and height */
	@Override
	protected void onMeasure(int widthspec, int heightspec) {
		int screenwidth = MeasureSpec.getSize(widthspec);
		// int screenheight = MeasureSpec.getSize(heightspec);
		WhiteKeyWidth = (int) (screenwidth / NUM_WHITE_KEYS);

		borderLR = (screenwidth - WhiteKeyWidth * NUM_WHITE_KEYS) / 2;
		borderT = 3;
		borderB = WhiteKeyWidth;
		WhiteKeyHeight = WhiteKeyWidth * 5;
		BlackKeyWidth = WhiteKeyWidth / 2;
		BlackKeyHeight = WhiteKeyHeight * 5 / 9;

		calcKeyPositions();

		int width = borderLR * 2 + WhiteKeyWidth * NUM_WHITE_KEYS;
		int height = borderT + borderB + WhiteKeyHeight;
		setMeasuredDimension(width, height);
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

	@Override
	protected void onSizeChanged(int newwidth, int newheight, int oldwidth, int oldheight) {
		super.onSizeChanged(newwidth, newheight, oldwidth, oldheight);
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
		bufferCanvas.translate(borderLR, borderT);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.WHITE);
		bufferCanvas.drawRect(0, 0, 0 + WhiteKeyWidth * NUM_WHITE_KEYS, WhiteKeyHeight, paint);
		paint.setStyle(Paint.Style.STROKE);

		for (int i = 0; i < NUM_KEYS; i++) {
			if (keyPositions[i] % WhiteKeyWidth == 0) {
				int x1 = keyPositions[i];
				// fill the key
				if (shadedKeys[i] != 0) {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(shade[0]);
					bufferCanvas.drawRect(x1, 0, x1 + WhiteKeyWidth, WhiteKeyHeight, paint);
					paint.setStyle(Paint.Style.STROKE);
				}
				// draw white key separation
				if (i > 0) {
					paint.setColor(gray1);
					bufferCanvas.drawLine(x1, 0, x1, WhiteKeyHeight, paint);
					paint.setColor(gray2);
					bufferCanvas.drawLine(x1 - 1, 1, x1 - 1, WhiteKeyHeight, paint);
					paint.setColor(gray3);
					bufferCanvas.drawLine(x1 + 1, 1, x1 + 1, WhiteKeyHeight, paint);
				}
			}
		}

		// draw black keys
		for (int i = 0; i < NUM_KEYS; i++) {
			if (keyPositions[i] % WhiteKeyWidth != 0) {
				int x1 = keyPositions[i];
				int x2 = x1 + BlackKeyWidth;

				// draw the three sides
				paint.setColor(gray1);
				bufferCanvas.drawLine(x1, 0, x1, BlackKeyHeight, paint);
				bufferCanvas.drawLine(x2, 0, x2, BlackKeyHeight, paint);
				bufferCanvas.drawLine(x1, BlackKeyHeight, x2, BlackKeyHeight, paint);
				paint.setColor(gray2);
				bufferCanvas.drawLine(x1 - 1, 0, x1 - 1, BlackKeyHeight + 1, paint);
				bufferCanvas.drawLine(x2 + 1, 0, x2 + 1, BlackKeyHeight + 1, paint);
				bufferCanvas.drawLine(x1 - 1, BlackKeyHeight + 1, x2 + 1, BlackKeyHeight + 1, paint);
				paint.setColor(gray3);
				bufferCanvas.drawLine(x1 - 2, 0, x1 - 2, BlackKeyHeight + 2, paint);
				bufferCanvas.drawLine(x2 + 2, 0, x2 + 2, BlackKeyHeight + 2, paint);
				bufferCanvas.drawLine(x1 - 2, BlackKeyHeight + 2, x2 + 2, BlackKeyHeight + 2, paint);

				// fill key
				if (shadedKeys[i] == 0) {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(gray1);
					bufferCanvas.drawRect(x1, 0, x2, BlackKeyHeight, paint);
					paint.setColor(gray2);
					bufferCanvas.drawRect(x1 + 1, BlackKeyHeight - BlackKeyHeight / 8, x1 + 1 + BlackKeyWidth - 2, BlackKeyHeight - BlackKeyHeight / 8 + BlackKeyHeight / 8, paint);
					paint.setStyle(Paint.Style.STROKE);
				} else {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(shade[0]);
					bufferCanvas.drawRect(x1, 0, x2, BlackKeyHeight, paint);
					paint.setStyle(Paint.Style.STROKE);
				}
			}
		}

		bufferCanvas.translate(-borderLR, -borderT);

		int PianoWidth = WhiteKeyWidth * NUM_WHITE_KEYS;
		paint.setStyle(Paint.Style.FILL);

		// draw the four borders
		paint.setColor(gray1);
		bufferCanvas.drawRect(0, 0, PianoWidth + borderLR * 2, borderT - 2, paint);
		bufferCanvas.drawRect(0, 0, borderLR, borderT + WhiteKeyHeight + borderB, paint);
		bufferCanvas.drawRect(0, borderT + WhiteKeyHeight, borderLR * 2 + PianoWidth, borderT + WhiteKeyHeight + borderB, paint);
		bufferCanvas.drawRect(borderLR + PianoWidth, 0, PianoWidth + borderLR * 2, borderT + WhiteKeyHeight + borderB, paint);

		paint.setColor(gray2);
		bufferCanvas.drawLine(borderLR, borderT - 1, borderLR + PianoWidth, borderT - 1, paint);

		bufferCanvas.translate(borderLR, borderT);

		// draw the gray bottoms of the white keys
		for (int i = 0; i < NUM_WHITE_KEYS; i++) {
			bufferCanvas.drawRect(i * WhiteKeyWidth + 1, WhiteKeyHeight + 2, i * WhiteKeyWidth + 1 + WhiteKeyWidth - 2, WhiteKeyHeight + 2 + borderLR / 2, paint);
		}

		bufferCanvas.translate(-borderLR, -borderT);

		canvas.drawBitmap(bufferBitmap, 0, 0, paint);
	}

	/**
	 * Find the Midi notes that occur in the current time. Shade those notes on
	 * the piano displayed. Un-shade the those notes played in the previous
	 * time.
	 */
	public synchronized void shadeKey(int notenumber, int shade) {
		shadedKeys[notenumber - KEY_NOTE_OFFSET] = shade;
		draw();
	}

	private int getNoteForPixel(float x, float y) {
		x -= borderLR;
		y -= borderT;
		if (y > 0 && y < BlackKeyHeight) {
			for (int i = 0; i < NUM_KEYS; i++) {
				if (keyPositions[i] % WhiteKeyWidth != 0) {
					if (x > keyPositions[i] && x < keyPositions[i] + BlackKeyWidth) {
						return i + KEY_NOTE_OFFSET;
					}
				}
			}
		}
		if (y > 0 && y < WhiteKeyHeight) {
			for (int i = 0; i < NUM_KEYS; i++) {
				if (keyPositions[i] % WhiteKeyWidth == 0) {
					if (x > keyPositions[i] && x < keyPositions[i] + WhiteKeyWidth) {
						return i + KEY_NOTE_OFFSET;
					}
				}
			}
		}
		return -1;
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

	/** When the Piano is touched, pause the midi player */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();

		int index, id;
		switch (action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			index = event.getActionIndex();
			id = event.getPointerId(index);
			touchPoints.put(id, new PointF(event.getX(index), event.getY(index)));
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			index = event.getActionIndex();
			id = event.getPointerId(index);
			touchPoints.remove(id);
			break;
		case MotionEvent.ACTION_MOVE:
			int length = event.getPointerCount();
			for (int i = 0; i < length; i++) {
				id = event.getPointerId(i);
				try {
					touchPoints.get(id).set(event.getX(i), event.getY(i));
				} catch (IndexOutOfBoundsException e) {
				}
			}
			break;
		}

		boolean[] newTouchedNotes = new boolean[NUM_KEYS];
		Iterator<Integer> iterator = touchPoints.keySet().iterator();
		while (iterator.hasNext()) {
			int key = iterator.next();

			PointF point = touchPoints.get(key);
			int note = getNoteForPixel(point.x, point.y);
			if (note != -1) {
				newTouchedNotes[note - KEY_NOTE_OFFSET] = true;
			}
		}
		for (int i = 0; i < NUM_KEYS; i++) {
			if (!touchedNotes[i] && newTouchedNotes[i]) {
				shadeKey(i + KEY_NOTE_OFFSET, 1);
			}
			if (touchedNotes[i] && !newTouchedNotes[i]) {
				shadeKey(i + KEY_NOTE_OFFSET, 0);
			}
		}
		touchedNotes = newTouchedNotes;

		//		switch (event.getAction()) {
		//		case MotionEvent.ACTION_DOWN:
		//		case MotionEvent.ACTION_MOVE:
		//		case MotionEvent.ACTION_POINTER_DOWN:
		//			int touchedNote = getNoteForPixel(event.getX(), event.getY()) - KEY_NOTE_OFFSET;
		//			if (!touchedNotes[touchedNote]) {
		//				// TODO
		//				shadeKey(touchedNote + KEY_NOTE_OFFSET, 1);
		//			}
		//			newTouchedNotes[touchedNote] = true;
		//		}
		//		for (int i = 0; i < NUM_KEYS; i++) {
		//			if (touchedNotes[i] && !newTouchedNotes[i]) {
		//				// TODO
		//				shadeKey(i + KEY_NOTE_OFFSET, 0);
		//			}
		//		}
		//		return true;
		return true;
	}
}
