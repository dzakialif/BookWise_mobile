package com.example.bookwise.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.bookwise.Constants;
import com.example.bookwise.databinding.ActivityPdfAddBinding;
import com.example.bookwise.databinding.ActivityPdfViewBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.joanzapata.pdfview.listener.OnPageChangeListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfViewActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfViewBinding binding;

    private String bookId;

    private static final String TAG = "PDF_VIEW_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get bookId from intent that we passed in intent
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");
        Log.d(TAG, "onCreate: BookId: "+bookId);

        loadBookDetails();

        //handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    private void loadBookDetails() {
        //Database reference to gey book detail e.g. book url using book id
        //Step 1 : Get Book Url using Book Id
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("book");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get book url
                        String pdfUrl = ""+snapshot.child("url").getValue();

                        //Step 2 : Load Pdf using that url from firebase storage
                        loadBookFromUrl(pdfUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadBookFromUrl(String bookUrl) {
        Log.d(TAG, "loadBookFromUrl: get PDF from storage");
        StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        reference.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        try {
                            // Simpan byte array ke file sementara
                            File tempFile = File.createTempFile("tempPdf", ".pdf");
                            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                fos.write(bytes);
                                // Muat PDF dari file sementara menggunakan PDFView
                                binding.pdfView.fromFile(tempFile)
                                        // Remove or adjust the .pages() method call
                                        .enableSwipe(true)
                                        .swipeVertical(true)
                                        .onPageChange(new OnPageChangeListener() {
                                            @Override
                                            public void onPageChanged(int page, int pageCount) {
                                                int currentPage = (page + 1);
                                                binding.toolbarSubtitleTv.setText(currentPage + "/" + pageCount);
                                                Log.d(TAG, "onPageChanged: " + currentPage + "/" + pageCount);
                                            }
                                        })
                                        .load();


                                binding.progressBar.setVisibility(View.GONE);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Tangani kesalahan saat menyimpan file sementara
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getMessage());
                        // Gagal memuat buku
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }
}