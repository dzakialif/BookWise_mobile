package com.example.bookwise;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.bookwise.databinding.ActivityPdfEditBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfEditActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfEditBinding binding;

    //book id get from intent started from AdapterPdfAdmin
    private String bookId;

    //progress dialog
    private ProgressDialog progressDialog;

    private ArrayList<String> categoryIdArrayList, categoryTitleArrayList;

    private static final String TAG = "BOOK_EDIT_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //book id get from intent started from AdapterPdfAdmin
        bookId = getIntent().getStringExtra("bookId");

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        loadCategories();
        loadBookInfo();

        //handle click, pick category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                categoryDialog();
            }
        });

        //handle click, go to previous screen
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click begin upload
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

    }

    private void loadBookInfo() {
        Log.d(TAG, "loadBookInfo: Loading book info");

        DatabaseReference refBooks = FirebaseDatabase.getInstance().getReference("book");
        refBooks.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get book info
                        selectedCategoryId = "" + snapshot.child("categoryId").getValue();
                        String description = "" + snapshot.child("description").getValue();
                        String title = "" + snapshot.child("title").getValue();

                        //set to views
                        binding.titleEt.setText(title);
                        binding.descriptionEt.setText(description);

                        Log.d(TAG, "onDataChange: Loading book category info");
                        DatabaseReference refBookCategory = FirebaseDatabase.getInstance().getReference("categories");
                        refBookCategory.child(selectedCategoryId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        //get category
                                        String category = "" + snapshot.child("category").getValue();

                                        //set to category text view
                                        binding.categoryTv.setText(category);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String title = "", description = "", author = "", isbn = "", publish = "";
    private void validateData(){
        //get data
        title = binding.titleEt.getText().toString().trim();
        description = binding.descriptionEt.getText().toString().trim();
        author = binding.authorEt.getText().toString().trim();
        isbn = binding.isbnEt.getText().toString().trim();
        publish = binding.publishEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(title)){
            Toast.makeText(this, "Enter Tittle...", Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(description)){
            Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(author)) {
            Toast.makeText(this, "Enter Author Name...", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(isbn)) {
            Toast.makeText(this, "Enter ISBN...", Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(publish)){
            Toast.makeText(this, "Enter Year Published...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(selectedCategoryId)){
            Toast.makeText(this, "Pick Category", Toast.LENGTH_SHORT).show();
        }else {
            // Check if category is not changed
            if (selectedCategoryId.equals("null")) {
                Toast.makeText(this, "Change the category", Toast.LENGTH_SHORT).show();
            } else {
                updatePdf();
            }
        }
    }

    private void updatePdf(){
        Log.d(TAG, "updatePdf: Starting updating book info to db...");

        //show progress dialog
        progressDialog.setMessage("Updating book info...");
        progressDialog.show();

        //setup data update to db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("title", ""+title);
        hashMap.put("description", ""+description);
        hashMap.put("author", ""+author);
        hashMap.put("isbn", ""+isbn);
        hashMap.put("publish", ""+publish);
        hashMap.put("categoryId", ""+selectedCategoryId);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("book");
        ref.child(bookId)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Book updated...");
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, "Book info updated...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: failed to update due to " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(PdfEditActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private String selectedCategoryId = "", selectedCategoryTitle = "";

    private void categoryDialog(){
        //make string array from arraylist of string
        String[] categoriesArray = new String[categoryTitleArrayList.size()];
        for (int i = 0; i < categoryTitleArrayList.size(); i++){
            categoriesArray[i] = categoryTitleArrayList.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedCategoryId = categoryIdArrayList.get(which);
                        selectedCategoryTitle = categoryTitleArrayList.get(which);

                        //set to textview
                        binding.categoryTv.setText(selectedCategoryTitle);
                    }
                })
                .show();
    }

    private void loadCategories() {
        Log.d(TAG, "loadCategories: Loading categories...");
        categoryIdArrayList = new ArrayList<>();
        categoryTitleArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryIdArrayList.clear();
                categoryTitleArrayList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    String id = "" + ds.child("id").getValue();
                    String category = "" + ds.child("category").getValue();
                    categoryIdArrayList.add(id);
                    categoryTitleArrayList.add(category);

                    Log.d(TAG, "onDataChange: ID " +id);
                    Log.d(TAG, "onDataChange: Category " + category);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}