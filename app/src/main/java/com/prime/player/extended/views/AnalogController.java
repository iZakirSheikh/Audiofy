package com.prime.player.extended.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.ColorUtils;

/**
 */
public class AnalogController extends View {

    float midx, midy;
    Paint textPaint, circlePaint, circlePaint2, linePaint;
    String angle;
    float currdeg, deg = 3, downdeg;
    onProgressChangedListener mListener;
    private int accent = Color.RED;
    private int outerCircleColor = Color.parseColor("#222222");
    private int innerCircleColor = Color.parseColor("#000000");
    private int labelColor = Color.DKGRAY;
    private int progressActiveColor = accent;
    private int progressInactiveColor = ColorUtils.setAlphaComponent(accent, 96);
    private String label;

    public AnalogController(Context context) {
        super(context);
        init();
    }

    public AnalogController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalogController(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setOnProgressChangedListener(onProgressChangedListener listener) {
        mListener = listener;
    }

    void init() {
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(33);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        circlePaint = new Paint();
        circlePaint.setColor(outerCircleColor);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint2 = new Paint();
        circlePaint2.setColor(progressInactiveColor);
//        circlePaint2.setColor(Color.parseColor("#FFA036"));
        circlePaint2.setStyle(Paint.Style.FILL);
        linePaint = new Paint();
        linePaint.setColor(accent);
//        linePaint.setColor(Color.parseColor("#FFA036"));
        linePaint.setStrokeWidth(7);
        angle = "0.0";
        label = "Label";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        midx = canvas.getWidth() / 2;
        midy = canvas.getHeight() / 2;

        int ang = 0;
        float x = 0, y = 0;
        int radius = (int) (Math.min(midx, midy) * ((float) 14.5 / 16));
        float deg2 = Math.max(3, deg);
        float deg3 = Math.min(deg, 21);
        for (int i = (int) (deg2); i < 22; i++) {
            float tmp = (float) i / 24;
            x = midx + (float) (radius * Math.sin(2 * Math.PI * (1.0 - tmp)));
            y = midy + (float) (radius * Math.cos(2 * Math.PI * (1.0 - tmp)));
            circlePaint.setColor(progressInactiveColor);
            canvas.drawCircle(x, y, ((float) radius / 15), circlePaint);
        }
        for (int i = 3; i <= deg3; i++) {
            float tmp = (float) i / 24;
            x = midx + (float) (radius * Math.sin(2 * Math.PI * (1.0 - tmp)));
            y = midy + (float) (radius * Math.cos(2 * Math.PI * (1.0 - tmp)));
            circlePaint2.setColor(progressActiveColor);
            canvas.drawCircle(x, y, ((float) radius / 15), circlePaint2);
        }

        float tmp2 = deg / 24;
        float x1 = midx + (float) (radius * ((float) 2 / 5) * Math.sin(2 * Math.PI * (1.0 - tmp2)));
        float y1 = midy + (float) (radius * ((float) 2 / 5) * Math.cos(2 * Math.PI * (1.0 - tmp2)));
        float x2 = midx + (float) (radius * ((float) 3 / 5) * Math.sin(2 * Math.PI * (1.0 - tmp2)));
        float y2 = midy + (float) (radius * ((float) 3 / 5) * Math.cos(2 * Math.PI * (1.0 - tmp2)));

        circlePaint.setColor(outerCircleColor);
        canvas.drawCircle(midx, midy, radius * ((float) 13 / 15), circlePaint);
        circlePaint.setColor(innerCircleColor);
        canvas.drawCircle(midx, midy, radius * ((float) 11 / 15), circlePaint);
        textPaint.setColor(labelColor);
        canvas.drawText(label, midx, midy + (float) (radius * 1.1), textPaint);
        linePaint.setColor(accent);
        canvas.drawLine(x1, y1, x2, y2, linePaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(!isEnabled())
            return true;
        mListener.onProgressChanged((int) (deg - 2));
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            float dx = e.getX() - midx;
            float dy = e.getY() - midy;
            downdeg = (float) ((Math.atan2(dy, dx) * 180) / Math.PI);
            downdeg -= 90;
            if (downdeg < 0) {
                downdeg += 360;
            }
            downdeg = (float) Math.floor(downdeg / 15);
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - midx;
            float dy = e.getY() - midy;
            currdeg = (float) ((Math.atan2(dy, dx) * 180) / Math.PI);
            currdeg -= 90;
            if (currdeg < 0) {
                currdeg += 360;
            }
            currdeg = (float) Math.floor(currdeg / 15);

            if (currdeg == 0 && downdeg == 23) {
                deg++;
                if (deg > 21) {
                    deg = 21;
                }
                downdeg = currdeg;
            } else if (currdeg == 23 && downdeg == 0) {
                deg--;
                if (deg < 3) {
                    deg = 3;
                }
                downdeg = currdeg;
            } else {
                deg += (currdeg - downdeg);
                if (deg > 21) {
                    deg = 21;
                }
                if (deg < 3) {
                    deg = 3;
                }
                downdeg = currdeg;
            }

            angle = String.valueOf(deg);
            invalidate();
            return true;
        }
        return e.getAction() == MotionEvent.ACTION_UP || super.onTouchEvent(e);
    }

    public int getProgress() {
        return (int) (deg - 2);
    }

    public void setProgress(int param) {
        if (param != deg - 2) {
            deg = param + 2;
            invalidate();
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String txt) {
        if (txt != null && !txt.equals(label)) {
            label = txt;
            invalidate();
        }
    }

    public void setCircleColors(int outer, int inner) {
        if (outer != outerCircleColor || inner != innerCircleColor) {
            outerCircleColor = outer;
            innerCircleColor = inner;
            invalidate();
        }
    }

    public void setLabelColor(int color){
        if (color != labelColor){
            labelColor = color;
            invalidate();
        }
    }

    public void setAccent(int color) {
        if (accent != color) {
            accent = color;
            progressActiveColor = accent;
            progressInactiveColor = ColorUtils.setAlphaComponent(accent, 96);
            invalidate();
        }
    }

    public interface onProgressChangedListener {
        void onProgressChanged(int progress);
    }

}