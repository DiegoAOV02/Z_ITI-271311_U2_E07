package com.z_iti_271311_u2_e07;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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

        // Dibuja el encerrado en la imagen procesada
        Bitmap resultBitmap = detectAndDrawEncirclements(bitmap);
        imgPhoto.setImageBitmap(resultBitmap);
    }

    private Bitmap detectAndDrawEncirclements(Bitmap bitmap) {
        // Crear un Canvas para dibujar sobre el Bitmap
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        // Lista de colores para los diferentes grupos
        int[] colors = {
                android.graphics.Color.RED,
                android.graphics.Color.BLUE,
                android.graphics.Color.GREEN,
                android.graphics.Color.YELLOW,
                android.graphics.Color.CYAN,
                android.graphics.Color.MAGENTA
        };
        final int[] colorIndex = {0};

        // Realizar OCR en la imagen completa
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    // Guardar los rectángulos detectados
                    List<Rect> detectedRects = new ArrayList<>();

                    // Añadir cada rectángulo detectado de "1" o "X" en detectedRects
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        String text = block.getText();
                        if (text.equals("1") || text.equalsIgnoreCase("X")) {
                            Rect boundingBox = block.getBoundingBox();
                            if (boundingBox != null) {
                                detectedRects.add(boundingBox);
                            }
                        }
                    }

                    // Agrupar los rectángulos y dibujar el grupo con colores
                    List<Rect> groupedRects = groupRectangles(detectedRects);
                    for (Rect rect : groupedRects) {
                        Paint paint = new Paint();
                        paint.setColor(colors[colorIndex[0] % colors.length]); // Selecciona el color para el grupo
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5);

                        // Dibuja el rectángulo para el grupo actual
                        canvas.drawRect(rect, paint);
                        colorIndex[0]++; // Cambia al siguiente color
                    }

                    // Actualizar la imagen con el encerrado
                    imgPhoto.setImageBitmap(mutableBitmap);
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error en OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        return mutableBitmap;
    }


    // Método para agrupar rectángulos
    private List<Rect> groupRectangles(List<Rect> rects) {
        List<Rect> groupedRects = new ArrayList<>();
        boolean[] used = new boolean[rects.size()];

        for (int i = 0; i < rects.size(); i++) {
            if (!used[i]) {
                Rect group = new Rect(rects.get(i));
                used[i] = true;

                for (int j = i + 1; j < rects.size(); j++) {
                    if (!used[j] && areRectsAdjacent(group, rects.get(j))) {
                        group.union(rects.get(j));
                        used[j] = true;
                    }
                }

                groupedRects.add(group);
            }
        }

        return groupedRects;
    }

    // Método para verificar si dos rectángulos son adyacentes
    private boolean areRectsAdjacent(Rect rect1, Rect rect2) {
        int buffer = 10; // Tolerancia para considerar adyacencia

        // Verificar si están en la misma fila o columna con una pequeña tolerancia
        return (Math.abs(rect1.top - rect2.top) < buffer && Math.abs(rect1.bottom - rect2.bottom) < buffer) ||
                (Math.abs(rect1.left - rect2.left) < buffer && Math.abs(rect1.right - rect2.right) < buffer);
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
}
