package com.example.bookwise.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bookwise.MyApplication;
import com.example.bookwise.R;
import com.example.bookwise.adapters.AdapterPdfFavorite;
import com.example.bookwise.databinding.ActivityPdfDetailBinding;
import com.example.bookwise.models.ModelPdf;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PdfDetailActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfDetailBinding binding;

    //pdf id, get from intent
    String bookId, bookTitle, bookUrl;

    boolean isInMyFavorite = false;

    private FirebaseAuth firebaseAuth;

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get data from intent e.g. bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        //at start hide download button, because we need book url that we will load later in function loadBookDetails();
//        binding.downloadBookBtn.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null){
            checkIsFavorite();
        }

        loadBookDetails();
        //increment book view count, whenever this page
        MyApplication.incrementBookViewCount(bookId);


        //handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, open to view pdf
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);
            }
        });

        //handle click, add/remove favorite
        binding.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseAuth.getCurrentUser() == null){
                    Toast.makeText(PdfDetailActivity.this, "You're Not Logged in", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (isInMyFavorite){
                        //in favorite, remove from favorite
                        MyApplication.removeFromFavorite(PdfDetailActivity.this, bookId);
                    }
                    else {
                        //not in favorite, add to favorite
                        MyApplication.addToFavorite(PdfDetailActivity.this, bookId);
                    }
                }
            }
        });

        //handle click, download pdf
//        binding.downloadBookBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG_DOWNLOAD, "onClick: Checking Permission");
//                if (ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
//                    Log.d(TAG_DOWNLOAD, "onClick: Permission already granted, can download book");
//                    MyApplication.downloadBook(PdfDetailActivity.this, ""+bookId, ""+bookTitle, ""+bookUrl);
//                }
//                else {
//                    Log.d(TAG_DOWNLOAD, "onClick: Permission was not granted, request permission...");
//                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                }
//
//            }
//        });
    }

    //request storage permission
//    private ActivityResultLauncher<String> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                if (isGranted){
//                    Log.d(TAG_DOWNLOAD, "Permission Granted");
//                    MyApplication.downloadBook(this, ""+bookId, ""+bookTitle, ""+bookUrl);
//                }
//                else {
//                    Log.d(TAG_DOWNLOAD, "Permission was denied... ");
//                    Toast.makeText(this, "Permission was denied...", Toast.LENGTH_SHORT).show();
//                }
//            });


    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("book");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle = ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String author = ""+snapshot.child("author").getValue();
                        String isbn = ""+snapshot.child("isbn").getValue();
                        String publish = ""+snapshot.child("publish").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();

                        //required data is loaded, show download button
//                        binding.downloadBookBtn.setVisibility(View.VISIBLE);

                        //formatdate
                        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        MyApplication.loadCategory(
                                ""+categoryId,
                                binding.categoryTv
                        );
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.pdfView,
                                binding.progressBar,
                                binding.pagesTv
                        );
                        MyApplication.loadPdfSize(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.sizeTv
                        );

                        //set data
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.viewsTv.setText(viewsCount.replace("null", "N/A"));
                        binding.authorTv.setText(author.replace("null", "N/A"));
                        binding.isbnTv.setText(isbn.replace("null", "N/A"));
                        binding.publishTv.setText(publish.replace("null", "N/A"));
                        binding.downloadsTv.setText(downloadsCount.replace("null", "N/A"));
                        binding.dateTv.setText(date);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIsFavorite(){
        //logged in check if its in favorite list or not
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("user");
        reference.child(firebaseAuth.getUid()).child("favorites").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavorite = snapshot.exists(); //true: if exist. false: if not exist
                        if (isInMyFavorite){
                            //exist in favorite
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite,0,0);
                            binding.favoriteBtn.setText("Remove Favorite");
                        }
                        else {
                            //not exist in favorite
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_unfavorite,0,0);
                            binding.favoriteBtn.setText("Add Favorite");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}