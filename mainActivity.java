package com.droidx.internship;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
//import com.google.firebase.FirebaseApp;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
//import com.google.mlkit.vision.text.TextRecognizerOptions;



public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Interpreter tflite;
    private ImageView imageView;
    private TextView textViewResult, textViewPrediction;
    private EditText editTextUsage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        FirebaseApp.initializeApp(this);

        Button buttonCapture = findViewById(R.id.buttonCapture);
        Button buttonPredict = findViewById(R.id.buttonPredict);

        imageView = findViewById(R.id.imageView);
        textViewResult = findViewById(R.id.textViewResult);
        textViewPrediction = findViewById(R.id.textViewPrediction);
        editTextUsage = findViewById(R.id.editTextUsage);

        // Load ML model
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e("MainActivity", "Error loading TensorFlow model", e);
        }

        // Capture bill and run OCR
        buttonCapture.setOnClickListener(v -> dispatchTakePictureIntent());

        // Predict electricity usage
        buttonPredict.setOnClickListener(v -> predictConsumption());
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(getAssets().openFd("model.tflite").getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(imageBitmap);
            runTextRecognition(imageBitmap);
        }
    }

    private void runTextRecognition(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    String extractedText = text.getText();
                    textViewResult.setText("Extracted: " + extractedText);
                    // Parse extractedText to get relevant information
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Text recognition failed", e));
    }

    private void predictConsumption() {
        String usageInput = editTextUsage.getText().toString();
        String[] usageStrings = usageInput.split(",");
        float[] inputs = new float[6];
        try {
            for (int i = 0; i < usageStrings.length; i++) {
                inputs[i] = Float.parseFloat(usageStrings[i].trim());
            }
        } catch (Exception e) {
            textViewPrediction.setText("Please enter valid usage data.");
            return;
        }

        float[][] output = new float[1][1];
        tflite.run(inputs, output);
        textViewPrediction.setText("Predicted usage: " + output[0][0]);
    }
}
