package com.z_iti_271311_u2_e07;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100; // Constante para identificar la solicitud de la cámara
    private static final int PERMISSION_REQUEST_CODE = 200; // Constante para identificar la solicitud de permisos
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
        btnCapture.setOnClickListener(view -> checkPermissionsAndOpenCamera());
    }

    /**
     * Verifica si el permiso de cámara está otorgado.
     * Si no lo está, solicita el permiso; de lo contrario, abre la cámara.
     */
    private void checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso de cámara
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            // Si el permiso ya está otorgado, abrir la cámara
            openCamera();
        }
    }

    /**
     * Maneja la respuesta de la solicitud de permisos.
     * Si el permiso de cámara fue otorgado, abre la cámara.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado, abrir la cámara
                openCamera();
            } else {
                // Permiso denegado, mostrar un mensaje al usuario
                Toast.makeText(this, "Permiso de cámara es necesario", Toast.LENGTH_SHORT).show();
            }
        }
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
        String imageFileName = "Mapa_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Ocultar el texto anterior
        tvResult.setText("");

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
        detectAndDrawEncirclements(bitmap);
    }

    private void detectAndDrawEncirclements(Bitmap bitmap) {
        Bitmap preprocessedBitmap = preprocessImage(bitmap);
        Bitmap mutableBitmap = preprocessedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        int[] colors = {
                android.graphics.Color.RED,
                android.graphics.Color.BLUE,
                android.graphics.Color.GREEN,
                android.graphics.Color.YELLOW,
                android.graphics.Color.CYAN,
                android.graphics.Color.MAGENTA
        };
        final int[] colorIndex = {0};

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    if (visionText.getTextBlocks().isEmpty()) {
                        Toast.makeText(MainActivity.this, "No se detectó texto en la imagen.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Rect> detectedRects = new ArrayList<>();
                    List<Rect> dontCareRects = new ArrayList<>();

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            for (Text.Element element : line.getElements()) {
                                String text = element.getText();
                                Rect boundingBox = element.getBoundingBox();
                                if (boundingBox != null) {
                                    if (text.equals("1")) {
                                        detectedRects.add(boundingBox);
                                    } else if (text.equalsIgnoreCase("X") || text.equals("*")) {
                                        dontCareRects.add(boundingBox);
                                    }
                                }
                            }
                        }
                    }

                    if (detectedRects.isEmpty() && dontCareRects.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No se detectaron elementos relevantes ('1' o 'X') en la imagen.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    groupRectanglesKarnaugh(detectedRects, dontCareRects, canvas, colors, colorIndex);
                    imgPhoto.setImageBitmap(mutableBitmap);
                    saveImageToFile(mutableBitmap);
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error en OCR: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void groupRectanglesKarnaugh(List<Rect> oneRects, List<Rect> dontCareRects, Canvas canvas, int[] colors, final int[] colorIndex) {
        // Combinar los rectángulos de '1' y 'X' para formar la cuadrícula
        List<Rect> allRects = new ArrayList<>();
        allRects.addAll(oneRects);
        allRects.addAll(dontCareRects);

        // Primero, clusterizar rectángulos en filas y columnas
        // Esto creará una cuadrícula donde cada celda representa una posible posición en el mapa

        // Ordenar rectángulos por coordenada Y central
        allRects.sort(Comparator.comparingInt(Rect::centerY));
        List<List<Rect>> rows = clusterRects(allRects, true); // Cluster por filas

        // Ordenar rectángulos por coordenada X central
        allRects.sort(Comparator.comparingInt(Rect::centerX));
        List<List<Rect>> cols = clusterRects(allRects, false); // Cluster por columnas

        int numRows = rows.size();
        int numCols = cols.size();
        int[][] grid = new int[numRows][numCols];
        Rect[][] gridRects = new Rect[numRows][numCols];

        // Mapear cada rectángulo a su posición en la cuadrícula
        for (int i = 0; i < numRows; i++) {
            List<Rect> row = rows.get(i);
            for (Rect rect : row) {
                int colIndex = getColumnIndex(rect, cols);
                if (colIndex != -1) {
                    if (oneRects.contains(rect)) {
                        grid[i][colIndex] = 1; // '1' marcado como 1
                    } else if (dontCareRects.contains(rect)) {
                        grid[i][colIndex] = 2; // 'X' marcado como 2
                    }
                    gridRects[i][colIndex] = rect;
                }
            }
        }

        // Implementar el algoritmo de agrupamiento
        boolean[][] visited = new boolean[numRows][numCols];
        List<List<int[]>> groups = new ArrayList<>();

        int maxGroupSize = 1;
        while (maxGroupSize <= numRows * numCols) {
            maxGroupSize *= 2;
        }
        maxGroupSize /= 2;

        for (int groupSize = maxGroupSize; groupSize >= 1; groupSize /= 2) {
            List<int[]> possibleDims = getPossibleDimensions(groupSize);
            for (int[] dim : possibleDims) {
                int rowsInGroup = dim[0];
                int colsInGroup = dim[1];
                for (int i = 0; i <= numRows - rowsInGroup; i++) {
                    for (int j = 0; j <= numCols - colsInGroup; j++) {
                        if (checkGroup(grid, visited, i, j, rowsInGroup, colsInGroup)) {
                            // Añadir grupo
                            List<int[]> group = new ArrayList<>();
                            for (int r = 0; r < rowsInGroup; r++) {
                                for (int c = 0; c < colsInGroup; c++) {
                                    group.add(new int[]{i + r, j + c});
                                    visited[i + r][j + c] = true;
                                }
                            }
                            groups.add(group);
                        }
                    }
                }
            }
        }

        // Crear variables
        int totalCells = numRows * numCols;
        int nVars = (int) (Math.log(totalCells) / Math.log(2));
        int nRowVars = (int) (Math.log(numRows) / Math.log(2));
        int nColVars = (int) (Math.log(numCols) / Math.log(2));

        String[] variableNames = {"A", "B", "C", "D"};
        String[] varsUsed = Arrays.copyOfRange(variableNames, 0, nVars);

        List<String> rowGrayCodes = generateGrayCode(nRowVars);
        List<String> colGrayCodes = generateGrayCode(nColVars);

        // Crear asignaciones de variables para cada celda
        CellInfo[][] cellVariableAssignments = new CellInfo[numRows][numCols];

        for (int i = 0; i < numRows; i++) {
            String rowCode = rowGrayCodes.get(i);
            for (int j = 0; j < numCols; j++) {
                String colCode = colGrayCodes.get(j);
                String code = rowCode + colCode;
                StringBuilder assignment = new StringBuilder();
                char[] bits = new char[nVars];
                for (int k = 0; k < code.length(); k++) {
                    char bit = code.charAt(k);
                    bits[k] = bit;
                    String var = varsUsed[k];
                    if (bit == '0') {
                        assignment.append(var).append("'");
                    } else {
                        assignment.append(var);
                    }
                }
                cellVariableAssignments[i][j] = new CellInfo(assignment.toString(), bits);
            }
        }

        // Lista para almacenar los términos booleanos
        List<String> terms = new ArrayList<>();

        // Dibujar rectángulos alrededor de los grupos y generar términos
        for (List<int[]> group : groups) {
            Rect groupRect = null;
            boolean containsOne = false;
            List<CellInfo> groupAssignments = new ArrayList<>();

            for (int[] pos : group) {
                Rect rect = gridRects[pos[0]][pos[1]];
                if (rect != null) {
                    if (groupRect == null) {
                        groupRect = new Rect(rect);
                    } else {
                        groupRect.union(rect);
                    }
                    if (grid[pos[0]][pos[1]] == 1) {
                        containsOne = true;
                    }
                    groupAssignments.add(cellVariableAssignments[pos[0]][pos[1]]);
                }
            }

            if (groupRect != null && containsOne) {
                Paint paint = new Paint();
                paint.setColor(colors[colorIndex[0] % colors.length]); // Selecciona el color para el grupo
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);

                // Dibuja el rectángulo para el grupo actual
                canvas.drawRect(groupRect, paint);
                colorIndex[0]++; // Cambia al siguiente color

                // Generar el término booleano para este grupo
                String term = getGroupTerm(groupAssignments, varsUsed);
                if (!term.isEmpty()) {
                    terms.add(term);
                }
            }
        }

        // Construir la expresión booleana final
        String booleanExpression = String.join(" + ", terms);

        // Mostrar la expresión en tvResult
        runOnUiThread(() -> tvResult.setText(booleanExpression));
    }

    private List<String> generateGrayCode(int n) {
        List<String> codes = new ArrayList<>();
        int numCodes = 1 << n; // 2^n codes
        for (int i = 0; i < numCodes; i++) {
            int gray = i ^ (i >> 1);
            String code = Integer.toBinaryString(gray);
            while (code.length() < n) {
                code = "0" + code;
            }
            codes.add(code);
        }
        return codes;
    }

    private String getGroupTerm(List<CellInfo> groupAssignments, String[] varsUsed) {
        if (groupAssignments.isEmpty()) {
            return "";
        }
        int nVars = varsUsed.length;
        char[] firstBits = groupAssignments.get(0).bits;
        StringBuilder term = new StringBuilder();
        for (int k = 0; k < nVars; k++) {
            char bit = firstBits[k];
            boolean same = true;
            for (int i = 1; i < groupAssignments.size(); i++) {
                if (groupAssignments.get(i).bits[k] != bit) {
                    same = false;
                    break;
                }
            }
            if (same) {
                if (bit == '0') {
                    term.append(varsUsed[k]).append("'");
                } else if (bit == '1') {
                    term.append(varsUsed[k]);
                }
            }
        }
        return term.toString();
    }

    private List<List<Rect>> clusterRects(List<Rect> rects, boolean isRow) {
        List<List<Rect>> clusters = new ArrayList<>();
        double threshold = 50.0; // Ajustar según sea necesario

        for (Rect rect : rects) {
            boolean added = false;
            for (List<Rect> cluster : clusters) {
                double clusterCoord = cluster.stream()
                        .mapToDouble(r -> isRow ? r.centerY() : r.centerX())
                        .average().orElse(0);
                double rectCoord = isRow ? rect.centerY() : rect.centerX();
                if (Math.abs(rectCoord - clusterCoord) < threshold) {
                    cluster.add(rect);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<Rect> newCluster = new ArrayList<>();
                newCluster.add(rect);
                clusters.add(newCluster);
            }
        }
        return clusters;
    }

    private int getColumnIndex(Rect rect, List<List<Rect>> cols) {
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).contains(rect)) {
                return i;
            }
        }
        return -1;
    }

    private List<int[]> getPossibleDimensions(int groupSize) {
        List<int[]> dimensions = new ArrayList<>();
        List<Integer> powersOfTwo = new ArrayList<>();
        int power = 1;
        while (power <= groupSize) {
            powersOfTwo.add(power);
            power *= 2;
        }
        for (int rows : powersOfTwo) {
            if (groupSize % rows == 0) {
                int cols = groupSize / rows;
                if (powersOfTwo.contains(cols)) {
                    dimensions.add(new int[]{rows, cols});
                }
            }
        }
        return dimensions;
    }

    private boolean checkGroup(int[][] grid, boolean[][] visited, int startRow, int startCol, int numRows, int numCols) {
        boolean containsOne = false;

        for (int i = startRow; i < startRow + numRows; i++) {
            for (int j = startCol; j < startCol + numCols; j++) {
                if (visited[i][j]) {
                    return false;
                }
                int cellValue = grid[i][j];
                if (cellValue == 0) { // No hay '1' ni 'X'
                    return false;
                }
                if (cellValue == 1) { // Contiene al menos un '1'
                    containsOne = true;
                }
            }
        }
        return containsOne;
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
            Toast.makeText(this, "Error rotando la imagen", Toast.LENGTH_SHORT).show();
            return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    // Clase para almacenar la información de cada celda
    class CellInfo {
        String assignment;
        char[] bits;

        CellInfo(String assignment, char[] bits) {
            this.assignment = assignment;
            this.bits = bits;
        }
    }

    // Guardar imagen procesada en almacenamiento
    private void saveImageToFile(Bitmap bitmap) {
        // Verificar la versión de Android
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            saveImageToMediaStore(bitmap);
        } else {
            saveImageToExternalStorage(bitmap);
        }
    }

    // Guardar la imagen en MediaStore para Android 10 o superior
    private void saveImageToMediaStore(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MiAplicacion");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Toast.makeText(this, "Imagen guardada en la galería", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al guardar la imagen en MediaStore", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error al crear URI para guardar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    // Guardar la imagen en almacenamiento externo para versiones anteriores a Android 10
    private void saveImageToExternalStorage(Bitmap bitmap) {
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MiAplicacion");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = new File(storageDir, "IMG_" + timeStamp + ".jpg");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "Imagen guardada en: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap preprocessImage(Bitmap bitmap) {
        Bitmap grayBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return grayBitmap;
    }

}