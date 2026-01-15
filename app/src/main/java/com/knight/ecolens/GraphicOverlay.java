
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
    private final Paint textPaint;
//    private final Paint textBackgroundPaint;
    private final List<String> detectedLabels = new ArrayList<>(); // To store names

    //Scaling factors
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Box Paint
        boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f);
        boxPaint.setAntiAlias(true);

        //Text paint for labels
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30.0f);
        textPaint.setFakeBoldText(true);
//        textPaint.setAntiAlias(true);

    }

    public void setConfiguration(int imageWidth, int imageHeight){
        // Since the camera is usually rotated 90 degrees, we swap width/height to match the vertical screen orientation.

        synchronized (lock) {
            this.scaleX = (float) getWidth() / (float) imageHeight;
            this.scaleY = (float) getHeight() / (float) imageWidth;
        }
    }

        //This method is called by the AI logic to update the squares on the screen.

        public void updateObjects(List<RectF> objects, List<String> labels){
            synchronized (lock){
                detectedObjects.clear();
                detectedLabels.clear();

                for(int i = 0; i < objects.size(); i++){
                    //Mapping the coordinates from AI-space to Screen-space
                    RectF rect = objects.get(i);
                    float left = rect.left * scaleX;
                    float top = rect.top * scaleY;
                    float right = rect.right * scaleX;
                    float bottom = rect.bottom * scaleY;
                    //adding box on the screen
                    detectedObjects.add(new RectF(left, top, right, bottom));
                    //adding label corresponding to the box
                    detectedLabels.add(labels.get(i));
                }
            }
            // Force the View to redraw itself on the screen
            postInvalidate();
        }

        @Override
        protected void onDraw(Canvas canvas){
            super.onDraw(canvas);

            synchronized (lock) {

                //log to check if we are actually trying to draw anything
                android.util.Log.d("GraphicOverlay", "Drawing" + detectedObjects.size() + "objects");
                for (int i=0; i<detectedObjects.size(); i++){
                    RectF rect = detectedObjects.get(i);
                    String label = detectedLabels.get(i);

                    //log to check the coordinates we are using to draw
                    android.util.Log.d("GraphicOverlay", "Drawing box at: " + rect.left + "," + rect.top + "Label: " + label);

                    //Dynamic Color Logic
                    if(label.contains("COMPOST")){
                        boxPaint.setColor(Color.GREEN);
                    }else if (label.contains("RECYCLE")) {
                        boxPaint.setColor(Color.GREEN);
                    } else if (label.contains("HAZARDOUS")) {
                        boxPaint.setColor(Color.GREEN);
                    } else{
                        boxPaint.setColor(Color.YELLOW);
                    }

                    Paint labelBgPaint = new Paint();
                    labelBgPaint.setColor(Color.parseColor("#80000000")); // semi transparent black
                    labelBgPaint.setStyle(Paint.Style.FILL);

                    float textWidth = textPaint.measureText(label);
                    // Drawing a small background rectangle
                    canvas.drawRect(rect.left, rect.top -55, rect.left + textWidth + 20, rect.top - 5, labelBgPaint);


                    //Draw the square on the canvas
                    canvas.drawRect(rect, boxPaint);

                    //Draw Label text slightly above the box
                    canvas.drawText(label, rect.left +10, rect.top - 25, textPaint);
                }
            }
        }


        //Interface to communicate the click back to MainActivity
        public interface OnObjectClickListener {
            void onObjectClick(String label, String category);
        }

        private OnObjectClickListener listener;

    public void setOnObjectClickListener(OnObjectClickListener listener){
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN){
            float x = event.getX();
            float y = event.getY();

            synchronized (lock){
                for (int i=0; i<detectedObjects.size(); i++){
                    RectF rect = detectedObjects.get(i);
                    // Checking if the touch coordinates (x,y) are inside this rectangle
                    if(rect.contains(x,y)){
                        if(listener != null){
                            listener.onObjectClick(detectedLabels.get(i), "CategoryPlaceholder");
                            return true; // click handled
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }



}
