
/*Why we are doing this:
Standard Android Views(like Buttons or TextViews) are "static." For AI,
we need to draw and erase squares 30 times per second. This class acts as
a blank canvas that we can draw manually using the onDraw method.
 */

package com.knight.ecolens;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/*
A transparent view that sits on top of the camera preview.
It is responsible for drawing the "Bounding Boxes" around detected objects.
*/
public class GraphicOverlay extends View{

    private final Object lock = new Object();
    private final List<RectF> detectedObjects = new ArrayList<>();
    private final Paint boxPaint;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Define how your "Bounding Box" looks
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
        boxPaint.setAntiAlias(true);
    }

        //This method is called by the AI logic to update the squares on the screen.

        public void updateObjects(List<RectF> objects){
            synchronized (lock){
                detectedObjects.clear();
                detectedObjects.addAll(objects);
            }
            // Force the View to redraw itself on the screen
            postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);

            synchronized (lock) {
                for (RectF rect : detectedObjects){
                    //Draw the square on the canvas
                    canvas.drawRect(rect, boxPaint);
                }
            }
        }


}
