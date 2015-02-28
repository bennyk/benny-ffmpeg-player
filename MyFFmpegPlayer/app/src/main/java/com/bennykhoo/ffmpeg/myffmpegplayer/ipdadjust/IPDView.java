package com.bennykhoo.ffmpeg.myffmpegplayer.ipdadjust;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by bennykhoo on 1/19/15.
 */

public class IPDView extends View {

    private static final String TAG = "IPDView";

    protected enum Corner {
        TL(1),
        TR(2),
        LL(3),
        LR(4);

        private Corner(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    public static final int TICK = 2;
    private int _dx = 0, _dy = 0;

    public IPDView(Context context) {
        super(context);
    }

    public IPDView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IPDView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getOffset() {
        return getWidth() / 2 + _dx;
    }

    public void setOffset(int offset) {
        _dx = offset - getWidth() / 2;
        invalidate();
    }

    public void adjustLeft() {
        _dx += -TICK;
        invalidate();
    }

    public void adjustRight() {
        _dx += TICK;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "using dx = " + _dx);
        final int ALIGN_MARKER_LEN = 30;
        final int ALIGN_OFFSET = 30;

        super.onDraw(canvas);
        int x = getWidth() / 2 + _dx;
        int y = getHeight() / 2 + _dy;

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawPaint(paint);
        // Use Color.parseColor to define HTML colors
//        paint.setColor(Color.parseColor("#CD5C5C"));
        paint.setColor(Color.WHITE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(5);
//        canvas.drawCircle(x, y, ALIGN_MARKER_LEN, paint);

        // draw center cross hair
        drawCross(canvas, x, y, ALIGN_MARKER_LEN, paint);

        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        // draw quartet align markers
        drawCornerAlign(canvas, getWidth() / 4   + _dx, getHeight() / 4   + _dy, ALIGN_MARKER_LEN, Corner.TL, paint);
        drawCornerAlign(canvas, getWidth() * 3/4 + _dx, getHeight() / 4   + _dy, ALIGN_MARKER_LEN, Corner.TR, paint);
        drawCornerAlign(canvas, getWidth() / 4   + _dx, getHeight() * 3/4 + _dy, ALIGN_MARKER_LEN, Corner.LL, paint);
        drawCornerAlign(canvas, getWidth() * 3/4 + _dx, getHeight() * 3/4 + _dy, ALIGN_MARKER_LEN, Corner.LR, paint);

        // draw align markers at the edge
        drawCornerAlign(canvas, 0 + _dx,          0 + _dy,           ALIGN_OFFSET, ALIGN_MARKER_LEN, Corner.TL, paint);
        drawCornerAlign(canvas, getWidth() + _dx, 0 + _dy,           ALIGN_OFFSET, ALIGN_MARKER_LEN, Corner.TR, paint);
        drawCornerAlign(canvas, 0 + _dx,          getHeight() + _dy, ALIGN_OFFSET, ALIGN_MARKER_LEN, Corner.LL, paint);
        drawCornerAlign(canvas, getWidth() + _dx, getHeight() + _dy, ALIGN_OFFSET, ALIGN_MARKER_LEN, Corner.LR, paint);
    }

    protected static void drawCross(Canvas canvas, int x, int y, int len, Paint paint) {
        canvas.drawLine(x - len, y, x + len, y, paint);
        canvas.drawLine(x, y - len, x, y + len, paint);
    }

    protected static void drawCornerAlign(Canvas canvas, int x, int y, int offset, int len, Corner corner, Paint paint) {
        switch (corner) {
            case TL:
                drawCornerAlign(canvas, x + offset, y + offset, len, corner, paint);
                break;

            case TR:
                drawCornerAlign(canvas, x - offset, y + offset, len, corner, paint);
                break;

            case LL:
                drawCornerAlign(canvas, x + offset, y - offset, len, corner, paint);
                break;

            case LR:
                drawCornerAlign(canvas, x - offset, y - offset, len, corner, paint);
                break;

        }
    }

        protected static void drawCornerAlign(Canvas canvas, int x, int y, int len, Corner corner, Paint paint) {
        Path p = new Path();

            switch (corner) {
                case TL: {
                    p.moveTo(x, y + len);
                    p.lineTo(x, y);
                    p.lineTo(x + len, y);
                    break;
                }

                case TR: {
                    p.moveTo(x, y + len);
                    p.lineTo(x, y);
                    p.lineTo(x - len, y);
                    break;
                }

                case LL: {
                    p.moveTo(x + len, y);
                    p.lineTo(x, y);
                    p.lineTo(x, y - len);
                    break;
                }

                case LR: {
                    p.moveTo(x - len, y);
                    p.lineTo(x, y);
                    p.lineTo(x, y - len);
                    break;
                }
            }
        canvas.drawPath(p, paint);

    }
}
