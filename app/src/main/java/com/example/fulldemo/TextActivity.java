package com.example.fulldemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class TextActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private boolean permission_granted=false;
    private StorageReference storageReference;
    EditText editText;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        askStoragePermissions();

        Button uploadBtn = findViewById(R.id.uploadBtn);
        editText=findViewById(R.id.editText);
        textView=findViewById(R.id.textView);

        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permission_granted)
                    uploadFileToFirebaseStorage(editText.getText().toString());
                else
                    Toast.makeText(TextActivity.this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadFileToFirebaseStorage(String string) {

        File externalDir = Environment.getExternalStorageDirectory();
        String fileName = "user_input.txt";
        File file = new File(externalDir, fileName);
        file.getParentFile().mkdirs();

        try {
            FileWriter writer = new FileWriter(file);
            writer.append(string);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        final ProgressDialog pd=ProgressDialog.show(this,"file upload","uploading...",true);
        StorageReference fileReference = storageReference.child("user_input.txt");

        try {
            InputStream stream = new FileInputStream(file);
            UploadTask uploadTask = fileReference.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(TextActivity.this, "Failed to upload", Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    pd.dismiss();
                    Toast.makeText(TextActivity.this, "Upload OK", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadFile(View view) throws IOException {
        final ProgressDialog pd=ProgressDialog.show(this,"File download","downloading...",true);
        StorageReference fileReference = storageReference.child("user_input.txt");
        final File localFile = File.createTempFile("aaa","txt");
        fileReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                pd.dismiss();
                Toast.makeText(TextActivity.this, "File download OK", Toast.LENGTH_LONG).show();
                String filePath = localFile.getPath();

                StringBuilder text = new StringBuilder();

                try {
                    FileInputStream fis = new FileInputStream(localFile);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line).append("\n");
                    }

                    br.close();
                    textView.setText(text.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(TextActivity.this, "File download failed", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void askStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            // Permission already granted
            permission_granted=true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                permission_granted=true;
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                startActivity(new Intent(TextActivity.this, GalleryActivity.class));
                break;
            case "Camera":
                startActivity(new Intent(TextActivity.this, CameraActivity.class));
                break;
            case "Text":
                startActivity(new Intent(TextActivity.this, TextActivity.class));
                break;
//            case "Map":
//                startActivity(new Intent(GalleryActivity.this, MapActivity.class));
//                break;
            case "Logout":
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(TextActivity.this, MainActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}