package com.knight.ecolens;

import static com.knight.ecolens.R.*;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import androidx.camera.core.ImageAnalysis;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import java.util.ArrayList;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
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
    private Button historyButton;
    private boolean isSheetVisible = false;

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

        historyButton = findViewById(id.btnHistory);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        previewView = findViewById(R.id.previewView);

        // check if we already have permission, if not ask for it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            previewView.post(() ->startCamera());
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

        graphicOverlay.setOnObjectClickListener((label, category) -> {
            //showing the detailed sheet when a box is clicked
            if(!isSheetVisible){
                isSheetVisible = true;
                showDetailsSheet(label, category);
            }
        });
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
                                    // telling the object the image size (for coordinate scaling)
                                    // we use width/height of the imageProxy
                                    android.util.Log.d("EcoLens", "Objects found: " + detectedObjects.size());
                                    graphicOverlay.setConfiguration(imageProxy.getWidth(), imageProxy.getHeight());

                                    List<RectF> boxes = new ArrayList<>();
                                    List<String> labels = new ArrayList<>();

                                    for(DetectedObject obj: detectedObjects){
                                        // Adding the bounding box of the detected object
                                        boxes.add(new RectF(obj.getBoundingBox()));

                                        //getting classification label (if available)
                                        String detectedLabel = "Unknown Item";
                                        //SAFE CHECK: Ensure labels exist before accessing them
                                        if(!obj.getLabels().isEmpty()){
                                            detectedLabel = obj.getLabels().get(0).getText();
                                             //like "food", "Fashion Good"
                                        }

                                        // Logic: Map the general label to a recycling category
                                        labels.add(mapToRecycling((detectedLabel)));
                                    }
                                    //pushing the boxes to our GraphicOverlay to draw them
                                    graphicOverlay.updateObjects(boxes, labels);
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

    private String mapToRecycling(String mlKitLabel){
        switch (mlKitLabel.toLowerCase()){
            case "food":
            case "plant":
            case "fruit":
            case "vegetable":
                return "COMPOSTABLE (Organic)";
            case "paper":
            case "cardboard":
            case "magazine":
            case "book":
                return "RECYCLE (Paper/Fiber)";
            case "can":
            case "metal":
            case "glass":
            case "cup":
                return "RECYCLE (Glass/metal";
            case "electronic":
            case "gadget":
            case "mobile":
            case "computer":
                return "HAZARDOUS (E-Waste Center)";
            default:
                return "SCANNING WASTE..."+mlKitLabel;
        }
    }

    private void showDetailsSheet(String label, String category) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        android.view.View view = getLayoutInflater().inflate(R.layout.layout_scan_result, null);

        TextView tvTitle = view.findViewById(R.id.tvDetectedObject);
        TextView tvInfo = view.findViewById(R.id.tvInstructions);
        Button btnDone = view.findViewById(R.id.btnConfirmLog);

        tvTitle.setText(label);
        String instructions = mapToRecycling(label); // Reusing existing mapping logic
        tvInfo.setText("Instructions: " + instructions);

        // DYNAMIC BUTTON COLOR LOGIC

        if(instructions.contains("COMPOSTABLE")) {
            btnDone.setBackgroundColor(android.graphics.Color.GREEN);
            btnDone.setText("Done (Compost)");
        }else if (instructions.contains("RECYCLE")){
            btnDone.setBackgroundColor(android.graphics.Color.BLUE);
            btnDone.setText("Done (Recycle)");
        }else if (instructions.contains("HAZARDOUS")){
            btnDone.setBackgroundColor(android.graphics.Color.BLACK);
            btnDone.setText("Done");
        }else{
            btnDone.setBackgroundColor(android.graphics.Color.GRAY);
            btnDone.setText("close");
        }
        // New: save to History only when this button is clicked
        btnDone.setOnClickListener(v ->{
                new Thread(()->{
                    try{
                        ///creating the item object
                        ScannedItem newItem = new ScannedItem(label, instructions, System.currentTimeMillis());

                        /// Showing a toast on the UI thread to confirm
                        runOnUiThread(()->{
                            Toast.makeText(MainActivity.this, "Item logged to history!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
    });

        dialog.setContentView(view);
        dialog.setOnDismissListener(d -> isSheetVisible = false);
        dialog.show();
    }
}

