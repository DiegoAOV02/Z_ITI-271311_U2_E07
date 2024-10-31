package com.z_iti_271311_u2_e07;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100; // Constante para identificar la solicitud de la cámara
    private TextView tvResult; // TextView para mostrar los resultados
    private Button btnCapture; // Botón de captura de imagen tomada con el dispositivo
    private ImageView imgPhoto; // ImageView para mostrar la imagen capturada
    private Uri photoUri; // URI de la imagen capturada
    private String currentPhotoPath; // Ruta de la imagen capturada

    // Carga la biblioteca de OpenCV al iniciar la aplicación
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV no se pudo cargar");
        } else {
            Log.d("OpenCV", "OpenCV se cargó correctamente");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar componentes de la interfaz de usuario
        tvResult = findViewById(R.id.tvResult);
        btnCapture = findViewById(R.id.btnCapture);
        imgPhoto = findViewById(R.id.imgPhoto);

        // Asignar evento clic al botón de captura
        btnCapture.setOnClickListener(view -> openCamera());
    }

    /**
     * Método para abrir la imagen y crear un archivo temporal para guardar la imagen
     * capturada y pasar la URI de la imagen al intent de la cámara
     */
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Intent para apertura de la cámara del dispositivo
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "com.z_iti_271311_u2_e07.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            setPic(); // Cargar y mostrar la imagen en el ImageView
        }
    }

    private void setPic() {
        int targetW = imgPhoto.getWidth();
        int targetH = imgPhoto.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        bitmap = rotateImageIfRequired(bitmap, currentPhotoPath);

        imgPhoto.setImageBitmap(bitmap); // Muestra la imagen en el ImageView

        detectVariablesAndProcessCells(bitmap); // Nueva función que integra la detección de variables y OCR
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String photoPath) {
        try {
            ExifInterface exif = new ExifInterface(photoPath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    private void detectVariablesAndProcessCells(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mat, mat, new Size(5, 5), 0);
        Imgproc.Canny(mat, mat, 50, 150);

        int numCells = detectGridCells(mat); // Función de OpenCV para detectar la cuadrícula
        int numVariables = determineNumberOfVariables(numCells);

        tvResult.setText("Número de Variables Detectadas: " + numVariables);

        // Procesar celdas con OCR
        recognizeCells(bitmap, numVariables);
    }

    private int detectGridCells(Mat mat) {
        // Aquí se haría la detección de contornos o cuadrícula
        // Devuelve el número de celdas detectadas (4, 8, 16, etc.)
        // Este es un código simulado, necesitarás ajustar con detección real
        return 16; // Supongamos que detectamos un mapa de Karnaugh de 4 variables
    }

    /**
     * Método para determinar el número de variables en el mapa de Karnaugh en base al número de
     * celdas que se tengan
     */
    private int determineNumberOfVariables(int numCells) {
        if (numCells == 4) return 2;
        if (numCells == 8) return 3;
        if (numCells == 16) return 4;
        if (numCells == 32) return 5;
        return -1;
    }

    private void recognizeCells(Bitmap bitmap, int numVariables) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    StringBuilder resultText = new StringBuilder();
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        resultText.append(block.getText()).append("\n");
                    }
                    tvResult.append("\nContenido detectado:\n" + resultText.toString());
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error en OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}