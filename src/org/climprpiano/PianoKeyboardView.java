package org.climprpiano;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class PianoKeyboardView extends View {

	private static final int MIDDLE_C = 60;

	public static final boolean[] IS_WHITE_KEY = new boolean[] { true, // C
			false, true, // D
			false, true, // E
			true, // F
			false, true, // G
			false, true, // A
			false, true // B
	};

	public static final int WHITE_KEY = 0;
	public static final int BLACK_KEY = 1;

	private static final float[] BLACK_KEY_X_OFFSETS = new float[] { 0, -0.6f, 0, -0.4f, 0, 0, -0.6f, 0, -0.5f, 0,
			-0.4f, 0 };

	public static int[][] calculateKeyPositions(int width, int startMidiNote, int numNotes) {
		int[][] keyPositions = new int[numNotes][2];
		int numWhiteKeys = 0;

		// count number of white keys
		for (int i = 0; i < numNotes; i++) {
			if (IS_WHITE_KEY[(startMidiNote + i) % 12]) {
				numWhiteKeys++;
			}
		}

		// calculate key positions
		int whiteKeyCnt = 0;
		for (int i = 0; i < numNotes; i++) {
			if (IS_WHITE_KEY[(startMidiNote + i) % 12]) {
				keyPositions[i] = new int[] { WHITE_KEY, width * whiteKeyCnt / numWhiteKeys,
						width * (whiteKeyCnt + 1) / numWhiteKeys };
				whiteKeyCnt++;
			} else {
				int position = (int) (width * (whiteKeyCnt + BLACK_KEY_X_OFFSETS[(startMidiNote + i) % 12] * 5 / 9) / numWhiteKeys);
				keyPositions[i] = new int[] { BLACK_KEY, position, position + keyPositions[0][2] * 5 / 9 };
			}
		}

		return keyPositions;
	}

	// the colors for the keys
	public static final int COLOR_LEFT = Color.BLUE;
	public static final int COLOR_RIGHT = Color.GREEN;

	private Paint paint; // The paint options for drawing
	private int width; // the width of the view
	private int height; // the height of the view

	private int keyOffset; // the most left keys midi note
	private int numKeys; // the number of keys
	private int[][] keyPositions; // an array containing for each key if it is white the y1 and y2 position
	private Map<Integer, Integer> shadedKeys; // the used color for each midiKey

	public PianoKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setAntiAlias(false);

		setDisplayedKeys(21, 108);
		shadeKeys(new HashMap<Integer, Integer>());
	}

	// sets the colors of multiple keys given by the map containing a color
	// numbers from 0=white to 8
	public void shadeKeys(Map<Integer, Integer> keyColors) {
		shadedKeys = keyColors;
		invalidate();
	}

	// sets the shown keys keyboard by a start and end midi note
	public void setDisplayedKeys(int startMidiNote, int endMidiNote) {
		// check if first note is a black key => always start with a white key
		if (!IS_WHITE_KEY[startMidiNote % 12]) {
			startMidiNote--;
		}

		// check if last note is a black key => always end with a white key
		if (!IS_WHITE_KEY[(endMidiNote) % 12]) {
			endMidiNote++;
		}

		keyOffset = startMidiNote;
		numKeys = endMidiNote - startMidiNote + 1;

		keyPositions = calculateKeyPositions(width, keyOffset, numKeys);
		invalidate();
	}

	/** Set the measured width and height */
	@Override
	protected void onMeasure(int widthspec, int heightspec) {
		width = MeasureSpec.getSize(widthspec);
		height = MeasureSpec.getSize(heightspec);

		keyPositions = calculateKeyPositions(width, keyOffset, numKeys);

		height = (int) ((3 * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources()
				.getDisplayMetrics()) + 4 * keyPositions[0][2]) / 4); // the height is calculated 25% from 4x the width of white key and 75% from 40dp

		setMeasuredDimension(width, height);
		invalidate();
	}

	private static final int gray1 = Color.rgb(16, 16, 16);
	private static final int gray2 = Color.rgb(90, 90, 90);
	private static final int gray3 = Color.rgb(200, 200, 200);

	/** Draw the Keyboard. */
	@Override
	protected void onDraw(Canvas canvas) {
		if (keyPositions == null) {
			return;
		}

		int borderTop = 2;
		int borderBottom = keyPositions[0][2] / 2; // the 3d-height is half the width
		int whiteKeyHeight = height - borderTop - borderBottom;
		int BlackKeyHeight = whiteKeyHeight * 5 / 9;

		canvas.translate(0, borderTop);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.WHITE);
		canvas.drawRect(0, 0, width, whiteKeyHeight, paint);
		paint.setColor(Color.BLACK);
		canvas.drawRect(0, whiteKeyHeight, width, height, paint);
		paint.setStyle(Paint.Style.STROKE);

		// draw white keys
		for (int i = 0; i < keyPositions.length; ++i) {
			int[] key = keyPositions[i];
			if (key[0] == WHITE_KEY) {
				// fill the key
				if (shadedKeys.containsKey(i + keyOffset)) {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(shadedKeys.get(i + keyOffset));
					canvas.drawRect(key[1], 0, key[2], whiteKeyHeight, paint);
				} else if (i + keyOffset == MIDDLE_C) {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(Color.rgb(255, 230, 190));
					canvas.drawRect(key[1], 0, key[2], whiteKeyHeight, paint);
				}
				// draw the grey bottom of the key
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(gray2);
				canvas.drawRect(key[1] + 1, whiteKeyHeight + 2, key[2] - 2, whiteKeyHeight + 2 + borderBottom, paint);
				// draw white key separation
				if (i > 0) {
					paint.setStyle(Paint.Style.STROKE);
					paint.setColor(gray1);
					canvas.drawLine(key[1], 0, key[1], whiteKeyHeight, paint);
					paint.setColor(gray2);
					canvas.drawLine(key[1] - 1, 1, key[1] - 1, whiteKeyHeight, paint);
					paint.setColor(gray3);
					canvas.drawLine(key[1] + 1, 1, key[1] + 1, whiteKeyHeight, paint);
				}
			}
		}

		// draw black keys
		for (int i = 0; i < keyPositions.length; ++i) {
			int[] key = keyPositions[i];
			if (key[0] == BLACK_KEY) {
				int x1 = key[1];
				int x2 = key[2];

				// draw the three sides
				paint.setStyle(Paint.Style.STROKE);
				paint.setColor(gray1);
				canvas.drawLine(x1, 0, x1, BlackKeyHeight, paint);
				canvas.drawLine(x2, 0, x2, BlackKeyHeight, paint);
				canvas.drawLine(x1, BlackKeyHeight, x2, BlackKeyHeight, paint);
				paint.setColor(gray2);
				canvas.drawLine(x1 - 1, 0, x1 - 1, BlackKeyHeight + 1, paint);
				canvas.drawLine(x2 + 1, 0, x2 + 1, BlackKeyHeight + 1, paint);
				canvas.drawLine(x1 - 1, BlackKeyHeight + 1, x2 + 1, BlackKeyHeight + 1, paint);
				paint.setColor(gray3);
				canvas.drawLine(x1 - 2, 0, x1 - 2, BlackKeyHeight + 2, paint);
				canvas.drawLine(x2 + 2, 0, x2 + 2, BlackKeyHeight + 2, paint);
				canvas.drawLine(x1 - 2, BlackKeyHeight + 2, x2 + 2, BlackKeyHeight + 2, paint);

				// fill key
				if (shadedKeys.containsKey(i + keyOffset)) {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(shadedKeys.get(i + keyOffset));
					canvas.drawRect(x1, 0, x2, BlackKeyHeight, paint);
				} else {
					paint.setStyle(Paint.Style.FILL);
					paint.setColor(gray1);
					canvas.drawRect(x1, 0, x2, BlackKeyHeight, paint);
					paint.setColor(gray2);
					canvas.drawRect(x1 + 1, BlackKeyHeight - BlackKeyHeight / 8, x2 - 1, BlackKeyHeight
							- BlackKeyHeight / 8 + BlackKeyHeight / 8, paint);
				}
			}
		}

		canvas.translate(0, -borderTop);

		// draw the top border
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.rgb(80, 30, 0));
		canvas.drawRect(0, 0, width, 1, paint);
		paint.setColor(Color.rgb(120, 40, 0));
		canvas.drawRect(0, 1, width, 2, paint);
	}
}
