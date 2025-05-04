package com.example.bisniskubisnismu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class FaceOverlay extends View {
    private Paint paint;
    private RectF faceBounds;

    public FaceOverlay(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(15f);
        faceBounds =  new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float boxWidth = w * 0.6f;
        float boxHeight = h * 0.4f;
        float left = (w - boxWidth) / 2.0f;
        float top = (h - boxHeight) / 4.0f;
        faceBounds.set(left, top, left + boxWidth, top + boxHeight);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(faceBounds,paint);
    }

    public RectF getFaceBounds(){
        return faceBounds;
    }
}
