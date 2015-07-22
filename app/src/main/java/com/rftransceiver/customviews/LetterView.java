package com.rftransceiver.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by rth on 15-7-22.
 */
public class LetterView extends View {
    /**
     * the height of letter in view
     */
    private int letterHeight;

    private final Paint paint = new Paint();

    private final String[] letters = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
            "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X",
            "Y", "Z", "#"
    };

    /**
     * the id of choosed letter in letters
     */
    private int chooseLetter = -1;

    private int lettersMarginEnd;

    public LetterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        lettersMarginEnd = (int)(15 * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(letterHeight == 0) {
            letterHeight =  getHeight() / letters.length;
        }
        for (int i = 0;i < letters.length;i++) {
            paint.setTextSize(16f);
            paint.setColor(Color.BLACK);
            paint.setAntiAlias(true);
            if(i == chooseLetter) {
                paint.setColor(Color.BLUE);
            }
            float x = getWidth() - lettersMarginEnd - paint.measureText(letters[i]);
            float y = letterHeight * (i + 1);
            canvas.drawText(letters[i],x,y,paint);
            paint.reset();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float downX = event.getX();
        float downY = event.getY();

        int preChoose = chooseLetter;
        int choose = (int)(downY / getHeight() * letters.length);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                reDraw(choose,preChoose);
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_CANCEL:
                reDraw(choose,preChoose);
                break;
            case MotionEvent.ACTION_UP:
                choose = -1;
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    private void reDraw(int choose,int preChoose) {
        if(choose != preChoose) {
            if(choose >= 0 && choose < letters.length) {
                chooseLetter = choose;
                invalidate();
            }
        }
    }
}
