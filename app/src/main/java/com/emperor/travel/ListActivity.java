package com.emperor.travel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ListActivity extends AppCompatActivity {

    private DatabaseReference databaseReference, adminReference;
    private FirebaseRecyclerAdapter <TravelDeal, DealsViewHolder> adapter;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private boolean isAdmin;

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso= new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken("271242601468-i028p0cf54bqf6fmsnc07o0b8atq9rqj.apps.googleusercontent.com")  //a
                .requestIdToken("271242601468-o4gogr87ecube22vc9i1tbi2amb7jqj0.apps.googleusercontent.com")
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    signIn();
                }
            }
        };
        databaseReference = FirebaseDatabase.getInstance().getReference().child("travel_deals");
        adminReference = FirebaseDatabase.getInstance().getReference().child("administrators");
        if (auth.getCurrentUser() != null) {
            checkAdmin(auth.getCurrentUser().getUid());
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        progressBar = findViewById(R.id.progressBar2);
        recyclerView = findViewById(R.id.rv_deals);
        recyclerView.setLayoutManager(layoutManager);

    }

    @Override
    protected void onStart() {
        super.onStart();
        initDatabase();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        auth.removeAuthStateListener(authStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.list, menu);
        MenuItem insertMenu = menu.findItem(R.id.insert);
        if (isAdmin) {
            insertMenu.setVisible(true);
        } else {
            insertMenu.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.insert) {
            Intent intent  = new Intent(ListActivity.this, DealsActivity.class);
            startActivity(intent);
        }
        if (item.getItemId() == R.id.logout) {
            auth.signOut();
            mGoogleSignInClient.signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    public void initDatabase(){
        FirebaseRecyclerOptions<TravelDeal> options = new FirebaseRecyclerOptions.Builder<TravelDeal>()
                .setQuery(databaseReference, TravelDeal.class)
                .build();
        adapter = new FirebaseRecyclerAdapter<TravelDeal, DealsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull DealsViewHolder dealsViewHolder, int i, @NonNull TravelDeal travelDeal) {
                String postID = getRef(i).getKey();
                dealsViewHolder.bind(postID, travelDeal, ListActivity.this);

                progressBar.setVisibility(View.GONE);
            }

            @NonNull
            @Override
            public DealsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rv_row, parent, false);
                return new DealsViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }
    public void signIn(){
        Intent intent = new Intent(ListActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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

    //
    public static class DealsViewHolder extends RecyclerView.ViewHolder{

        private TextView title, price, description;
        private ImageView imageView;

        public DealsViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tv_title);
            price = itemView.findViewById(R.id.tv_price);
            description = itemView.findViewById(R.id.tv_description);
            imageView = itemView.findViewById(R.id.imageDeal);
        }

        public void bind(final String postID, final TravelDeal travelDeal, final Context context){
            title.setText(travelDeal.getTitle());
            price.setText("$" + travelDeal.getPrice());
            description.setText(travelDeal.getDescription());

            RequestOptions requestOptions = new RequestOptions()
                    .error(context.getResources().getDrawable(R.drawable.img))
                    .placeholder(new ColorDrawable(context.getResources().getColor(R.color.colorPrimary)));
            Glide.with(context)
                    .load(travelDeal.getImage_url())
                    .apply(requestOptions)
                    .into(imageView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, DealsActivity.class);
                    intent.putExtra("dealTitle", travelDeal.getTitle());
                    intent.putExtra("dealPrice", travelDeal.getPrice());
                    intent.putExtra("dealDescription", travelDeal.getDescription());
                    intent.putExtra("dealImage", travelDeal.getImage_url());
                    intent.putExtra("postID", postID);

                    context.startActivity(intent);
                }
            });
        }
    }
}
