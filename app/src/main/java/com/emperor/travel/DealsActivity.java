package com.emperor.travel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class DealsActivity extends AppCompatActivity {

    private Task task;
    private DatabaseReference mDatabaseReference, deals, adminReference;
    private StorageReference mStorageReference;
    private FirebaseAuth auth;
    private EditText edTitle, edPrice, edDescription;
    private ProgressDialog dialog;
    private String msg;
    private boolean op = false;
    private boolean isAdmin;
    private static final int RC = 42;
    private TravelDeal travelDeal = new TravelDeal();

    private Button btnImage;
    private ImageView imageView;
    private Map<String, Object> map = new HashMap<>();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deals);

        mStorageReference = FirebaseStorage.getInstance().getReference().child("deals_pictures");
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("travel_deals");
        adminReference = FirebaseDatabase.getInstance().getReference().child("administrators");
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            checkAdmin(auth.getCurrentUser().getUid());
        }

        edTitle = findViewById(R.id.ed_title);
        edPrice = findViewById(R.id.ed_price);
        edDescription = findViewById(R.id.ed_description);
        btnImage = findViewById(R.id.btn_image);
        imageView = findViewById(R.id.imageView);

        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent, "Insert Picture"), RC);
            }
        });

        displayEditValues();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            imageView.setImageURI(imageUri);
            StorageReference imageRef = mStorageReference.child(imageUri.getLastPathSegment());
            findViewById(R.id.progressBar3).setVisibility(View.VISIBLE);
            imageRef.putFile(imageUri)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        findViewById(R.id.progressBar3).setVisibility(View.GONE);
                        Task<Uri> downloadUri = task.getResult().getMetadata().getReference().getDownloadUrl();
                        downloadUri.addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String url = task.getResult().toString();
//                                    travelDeal.setImage_url(url);
                                    map.put("image_url", url);

                                }
                            }
                        });
                    }
                });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        if (isAdmin) {
            menu.findItem(R.id.save).setVisible(true);
            menu.findItem(R.id.delete).setVisible(true);
            btnImage.setVisibility(View.VISIBLE);
            enableFields(true);
        }else{
            menu.findItem(R.id.save).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            btnImage.setVisibility(View.INVISIBLE);
            enableFields(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.save) {
            saveDeal();
        }
        if (item.getItemId() == R.id.delete) {
            deleteDeal();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Intent intent = getIntent();
        if (intent != null) {
            if (!intent.hasExtra("postID")) {
                menu.getItem(0).setTitle("SAVE");
                menu.getItem(1).setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void displayEditValues() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("postID")) {
                edTitle.setText(intent.getExtras().getString("dealTitle", ""));
                edPrice.setText(intent.getExtras().getString("dealPrice", ""));
                edDescription.setText(intent.getExtras().getString("dealDescription", ""));

                RequestOptions requestOptions = new RequestOptions()
                        .error(getResources().getDrawable(R.drawable.img))
                        .placeholder(new ColorDrawable(getResources().getColor(R.color.colorPrimary)));
                Glide.with(getApplicationContext())
                        .load(intent.getExtras().getString("dealImage", ""))
                        .apply(requestOptions)
                        .into(imageView);
            }
        }
    }

    private void saveDeal() {
        String title = edTitle.getText().toString();
        String price = edPrice.getText().toString();
        String description = edDescription.getText().toString();
        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Enter Deal Title ...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(price)) {
            Toast.makeText(this, "Enter Price ...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Enter Description ...", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog = new ProgressDialog(this);
        dialog.setTitle("Please wait ...");
        dialog.setMessage("Saving deal ...");
        dialog.setCancelable(false);

        msg = "Deal saved ...";
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("postID")) {
                deals = mDatabaseReference.child(intent.getExtras().getString("postID"));
                map.put("title", title);
                map.put("price", price);
                map.put("description", description);
                task = deals.updateChildren(map);
                dialog.setMessage("Updating deal ...");
                msg = "Deal updated ...";
                op = true;
            } else {
                deals = mDatabaseReference.push();
                map.put("title", title);
                map.put("price", price);
                map.put("description", description);
                task = deals.setValue(map);
            }
        } else {
            deals = mDatabaseReference.push();
            map.put("title", title);
            map.put("price", price);
            map.put("description", description);
            task = deals.setValue(map);
        }
        dialog.show();

//        TravelDeal travelDeal = new TravelDeal(title, price, description, "");
//        travelDeal.setTitle(title);
//        travelDeal.setPrice(price);
//        travelDeal.setDescription(description);
//        deals.setValue(travelDeal)
        task
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    dialog.dismiss();
                    Toast.makeText(DealsActivity.this, msg, Toast.LENGTH_SHORT).show();
                    edTitle.setText("");
                    edPrice.setText("");
                    edDescription.setText("");
//                    if (op)
                        finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(DealsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void deleteDeal() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("postID")) {
                dialog = new ProgressDialog(this);
                dialog.setMessage("Deleting deal");
                dialog.setTitle("Please wait ...");
                dialog.setCancelable(false);
                dialog.show();

                mDatabaseReference.child(getIntent().getExtras().getString("postID")).removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            dialog.dismiss();
                            Toast.makeText(DealsActivity.this, "Deal Deleted ...", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(DealsActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }


    }

    private void checkAdmin(String uid){
        isAdmin = false;
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()){
                    isAdmin = true;
                    invalidateOptionsMenu();
                    Log.d("Admin", "User is an administrator");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        adminReference.child(uid).addChildEventListener(childEventListener);
    }
    private void enableFields(boolean isEnabled){
        edTitle.setEnabled(isEnabled);
        edDescription.setEnabled(isEnabled);
        edPrice.setEnabled(isEnabled);
    }
}
