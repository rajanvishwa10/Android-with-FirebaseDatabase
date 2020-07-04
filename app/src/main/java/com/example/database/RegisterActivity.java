package com.example.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private static final int PICK_IMAGE = 1;
    private CircleImageView circleImageView;
    Uri uri;
    static int PReqCode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("SIGN IN");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("REGISTERING");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(RegisterActivity.this, R.color.colorRed));
        }
        mAuth = FirebaseAuth.getInstance();
        circleImageView = (CircleImageView) findViewById(R.id.profile_image);
        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    public void sign(View view) {

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void sig(View view) {
        progressDialog.show();
        EditText editText = findViewById(R.id.email);
        EditText editText1 = findViewById(R.id.pass);
        EditText editText2 = findViewById(R.id.conpass);
        EditText editText3 = findViewById(R.id.name);
        final String email = editText.getText().toString();
        final String pass = editText1.getText().toString();
        String confirm = editText2.getText().toString();
        final String name = editText3.getText().toString();
        if (email.isEmpty()) {
            editText.setError("Enter email");
            progressDialog.dismiss();
        } else if (pass.isEmpty()) {
            progressDialog.dismiss();
            editText1.setError("Enter Password");
        } else if (confirm.isEmpty()) {
            progressDialog.dismiss();
            editText2.setError("Enter Password");
        } else if (pass.length() < 6 || confirm.length() < 6) {
            progressDialog.dismiss();
            Toast.makeText(this, "Password must be greater than 6", Toast.LENGTH_SHORT).show();
        } else if (name.isEmpty()) {
            progressDialog.dismiss();
            Toast.makeText(this, "Enter Name", Toast.LENGTH_SHORT).show();
        } else if (!pass.equals(confirm)) {
            progressDialog.dismiss();
            Toast.makeText(this, "Password should match", Toast.LENGTH_LONG).show();
        } else if (pass.equals(confirm)) {
            createUser(name,email,pass);
        }

    }

    private void createUser(final String name, final String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            User user = new User(name, email);
                            FirebaseDatabase.getInstance().getReference("Demo")
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        addimage(name,uri,mAuth.getCurrentUser());
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();

                                        Toast.makeText(RegisterActivity.this, "Registered", Toast.LENGTH_SHORT).show();
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(RegisterActivity.this, "Image not uploaded", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(RegisterActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException)
                                Toast.makeText(getApplicationContext(), "Registeration failed ",
                                        Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();

                        }
                        // ...
                    }
                });
    }


    private void addimage(final String name, final Uri uri, final FirebaseUser firebaseUser) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("users_photo");
        StorageReference imageFile = storageReference.child(uri.getLastPathSegment());
        imageFile.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .setPhotoUri(uri)
                        .build();
                firebaseUser.updateProfile(profileChangeRequest)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Register Completed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }


    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && requestCode == RESULT_OK) {
            uri = data.getData();
            circleImageView.setImageURI(uri);
        } else {

        }
    }
}
