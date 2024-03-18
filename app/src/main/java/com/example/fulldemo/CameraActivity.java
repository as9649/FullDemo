package com.example.fulldemo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class CameraActivity extends AppCompatActivity {

    public static final int CAMERA_PERM_CODE = 202;
    public static final int REQUEST_IMAGE_CAPTURE = 303;

    private String currentImagePath;
    private String imageDate;
    private StorageReference storageReference;
    private StorageReference imageRef;

    private File localFile;

    private ImageView imageView2;
    private Button uploadButton2, downloadButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView2=findViewById(R.id.imageView2);
        uploadButton2=findViewById(R.id.uploadButton2);
        downloadButton2=findViewById(R.id.downloadButton2);

        storageReference= FirebaseStorage.getInstance().getReference();

        uploadButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermissions();
            }
        });
    }

    private void askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        else
            TakePictureIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
            else
                TakePictureIntent();
        }
    }

    private void TakePictureIntent() {

        // creating local temporary file to store the full resolution photo
        String filename = "tempfile";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            File imageFile = File.createTempFile(filename,".jpg",storageDir);
            currentImagePath = imageFile.getAbsolutePath();
            Uri imageUri = FileProvider.getUriForFile(CameraActivity.this, "com.example.fulldemo.fileprovider", imageFile);

            Intent takePictureIntent = new Intent();
            takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

        } catch (IOException e){
            Toast.makeText(CameraActivity.this,"Failed to create temporary file",Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);}
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data_back){
        super.onActivityResult(requestCode, resultCode, data_back);

        if (resultCode == Activity.RESULT_OK) {

            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");

            final ProgressDialog pd;

            // Upload camera full resolution image file
            if (requestCode == REQUEST_IMAGE_CAPTURE){
                pd=ProgressDialog.show(this,"Upload image","Uploading...",true);

                imageDate = dateFormat.format(date);
                imageRef = storageReference.child("Full").child(imageDate + ".jpg");

                Bitmap imageBitmap = BitmapFactory.decodeFile(currentImagePath);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                imageRef.putBytes(data)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                pd.dismiss();
                                Toast.makeText(CameraActivity.this, "Image Uploaded", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                pd.dismiss();
                                Toast.makeText(CameraActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }

    public void download2(View view) {
        imageRef = storageReference.child("Full").child(imageDate + ".jpg");

        final ProgressDialog pd=ProgressDialog.show(this,"Image download","downloading...",true);

        try {
            localFile = File.createTempFile(imageDate,"jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        imageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                pd.dismiss();
                Toast.makeText(CameraActivity.this, "Image download success", Toast.LENGTH_LONG).show();
                String filePath = localFile.getPath();
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                imageView2.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(CameraActivity.this, "Image download failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        String st = Objects.requireNonNull(item.getTitle()).toString();

        switch (st) {
            case "Gallery":
                startActivity(new Intent(CameraActivity.this, GalleryActivity.class));
                break;
            case "Camera":
                startActivity(new Intent(CameraActivity.this, CameraActivity.class));
                break;
//            case "Text":
//                startActivity(new Intent(GalleryActivity.this, TextActivity.class));
//                break;
//            case "Map":
//                startActivity(new Intent(GalleryActivity.this, MapActivity.class));
//                break;
            case "Logout":
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CameraActivity.this, MainActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}