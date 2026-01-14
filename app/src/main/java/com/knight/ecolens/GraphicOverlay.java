
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

    //Scaling factors
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Define how your "Bounding Box" looks
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
        boxPaint.setAntiAlias(true);
    }

    public void setConfiguration(int imageWidth, int imageHeight){
        // Since the camera is usually rotated 90 degrees, we swap width/height to match the vertical screen orientation.

        synchronized (lock) {
            this.scaleX = (float) getWidth() / (float) imageHeight;
            this.scaleY = (float) getHeight() / (float) imageWidth;
        }
    }

        //This method is called by the AI logic to update the squares on the screen.

        public void updateObjects(List<RectF> objects){
            synchronized (lock){
                detectedObjects.clear();

                for(RectF rect: objects){
                    //Mapping the coordinates from AI-space to Screen-space
                    float left = rect.left * scaleX;
                    float top = rect.top * scaleY;
                    float right = rect.right * scaleX;
                    float bottom = rect.bottom * scaleY;
                    detectedObjects.add(new RectF(left, top, right, bottom));
                }
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
