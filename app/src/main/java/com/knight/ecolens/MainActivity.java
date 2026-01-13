package com.knight.ecolens;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import androidx.camera.core.ImageAnalysis;
import android.graphics.RectF;
import java.util.ArrayList;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ObjectDetector objectDetector;
    private GraphicOverlay graphicOverlay;
    private PreviewView previewView;

    // A modern way to ask for permissions in 2026
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted){
                    // using a handler to wait 200ms for the OS to  release the camera lock
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::startCamera, 200);
//                    startCamera();
                }else{
                    Toast.makeText(this, "Camera permission is required to scan waste", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);

        // check if we already have permission, if not ask for it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            startCamera();
        }else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        //Logic for AI Detection Options
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE) //Fast for video
                .enableMultipleObjects()
                .enableClassification()     //Tries to identify what the object is
                .build();

        // Initialize the object detector
        objectDetector = ObjectDetection.getClient(options);
        graphicOverlay = findViewById(R.id.graphicOverlay);
    }
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(()->{
            try{
                //used to bind the lifecycle of camera to the lifecycle owner
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Getting the PreviewView from layer
                previewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

                // preview
                Preview preview = new Preview.Builder().build();

                // Connecting it to surface
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Select back camera as a default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();

                // Analysis Use Case (The AI Brain)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Performance
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    // Convert Camera frame to ML Kit format
                    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
                    android.media.Image mediaImage = imageProxy.getImage();

                    if(mediaImage != null){
                        InputImage image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.getImageInfo().getRotationDegrees()
                        );

                        // Process the image
                        objectDetector.process(image)
                                .addOnSuccessListener(detectedObjects -> {
                                    List<RectF> boxes = new ArrayList<>();
                                    for(DetectedObject obj: detectedObjects){
                                        // Adding the bounding box of the detected object
                                        boxes.add(new RectF(obj.getBoundingBox()));
                                    }
                                    //pushing the boxes to our GraphicOverlay to draw them
                                    graphicOverlay.updateObjects(boxes);
                                })
                                .addOnFailureListener(e -> e.printStackTrace())
                                .addOnCompleteListener(result -> {
                                    // Closing the image proxy
                                    imageProxy.close();
                                });
                    }
                });

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}