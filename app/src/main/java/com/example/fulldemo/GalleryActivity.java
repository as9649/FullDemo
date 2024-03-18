package com.example.fulldemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class GalleryActivity extends AppCompatActivity {
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 101;
    private final int Gallery=1;
    private boolean permission_granted=false;
    private ImageView imageView1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        imageView1=findViewById(R.id.imageView1);
        Button uploadButton1 = findViewById(R.id.uploadButton1);

        askStoragePermissions();
        uploadButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permission_granted)
                    uploadFromGallery();
                else
                    Toast.makeText(GalleryActivity.this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
        else
            permission_granted=true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
            else
                permission_granted=true;
        }
    }
    public void uploadFromGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(gallery, Gallery);
    }

    //Uploading selected image file to Firebase Storage
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Gallery) {

                Uri file = data.getData();

                if (file != null) {

                    final ProgressDialog pd=ProgressDialog.show(this,"Upload image","Uploading...",true);

                    StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("Images").child("aaa.jpg");
                    imageRef.putFile(file)
                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    pd.dismiss();
                                    Toast.makeText(GalleryActivity.this, "Image Uploaded", Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    pd.dismiss();
                                    Toast.makeText(GalleryActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                                }
                            });
                }
                else
                    Toast.makeText(this, "No Image was selected", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void download(View view) throws IOException {
        final ProgressDialog pd=ProgressDialog.show(this,"Image download","downloading...",true);

        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("Images").child("aaa.jpg");

        final File localFile = File.createTempFile("aaa","jpg");
        imageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                pd.dismiss();
                Toast.makeText(GalleryActivity.this, "Image download success", Toast.LENGTH_LONG).show();
                String filePath = localFile.getPath();
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                imageView1.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                pd.dismiss();
                Toast.makeText(GalleryActivity.this, "Image download failed", Toast.LENGTH_LONG).show();
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
                startActivity(new Intent(GalleryActivity.this, GalleryActivity.class));
                break;
            case "Camera":
                startActivity(new Intent(GalleryActivity.this, CameraActivity.class));
                break;
            case "Text":
                startActivity(new Intent(GalleryActivity.this, TextActivity.class));
                break;
//            case "Map":
//                startActivity(new Intent(GalleryActivity.this, MapActivity.class));
//                break;
            case "Logout":
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(GalleryActivity.this, MainActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}