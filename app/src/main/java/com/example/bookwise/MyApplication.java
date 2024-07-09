package com.example.bookwise;

import static com.example.bookwise.Constants.MAX_BYTES_PDF;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bookwise.adapters.AdapterPdfAdmin;
import com.example.bookwise.models.ModelPdf;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.joanzapata.pdfview.PDFView;
import com.joanzapata.pdfview.listener.OnLoadCompleteListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

//application class runs before your launcher activity
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    //created a static method to convert timestamp to proper date format
    public static final String formatTimestamp(long timestamp){
        Calendar cal =  Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);
        //format timestamp to dd/MM/yyyy
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();

        return date;
    }

    public static void deleteBook(Context context, String bookId, String bookUrl, String bookTitle) {
        String TAG = "DELETE_BOOK_TAG";

        Log.d(TAG, "deleteBook: Deleting...");
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please Wait...");
        progressDialog.setMessage("Deleting " + bookTitle + " ...");
        progressDialog.show();

        Log.d(TAG, "deleteBook: Deleting from storage...");
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Deleted from storage");

                        Log.d(TAG, "onSuccess: Now deleting info from db");
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("book");
                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Also deleted from db");
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "Book Deleted Successfully...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Failed to delet from db due to " + e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to delete from storage due to " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static void loadPdfSize(String pdfUrl, String bookTitle, TextView sizeTv) {
        String TAG = "BOOK_SIZE_TAG";
        //using url we can get file and its metadata from firebase storage

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get size in bytes
                        double bytes = storageMetadata.getSizeBytes();
                        Log.d(TAG, "onSuccess: "+bookTitle +" "+bytes);

                        //convert bytes to kb, mb
                        double kb = bytes/1024;
                        double mb = kb/1024;

                        if (mb >= 1){
                            sizeTv.setText(String.format("%.2f", mb)+" MB");
                        }else if (kb >= 1){
                            sizeTv.setText(String.format("%.2f", kb)+" KB");
                        }
                        else{
                            sizeTv.setText(String.format("%.2f", bytes)+" bytes");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed getting metadata
                        Log.d(TAG, "onFailure: "+e.getMessage());
                    }
                });
    }

    public static void loadPdfFromUrlSinglePage(String pdfUrl, String bookTitle, PDFView pdfView, ProgressBar progressBar) {
        // Using URL to get the file and its metadata from Firebase Storage
        String TAG = "BOOK_LOAD_SINGLE_TAG";

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);

        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(bytes -> {
                    Log.d(TAG, "onSuccess: " +bookTitle + " Successfully got the file");

                    // Save the PDF file to internal storage
                    String fileName = "temporary.pdf";
                    File file = new File(pdfView.getContext().getFilesDir(), fileName);
                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(bytes);
                        fos.close();

                        // Load PDF from the saved file
                        pdfView.fromFile(file)
                                .defaultPage(0) // show only the first page
                                .enableSwipe(true) // enable swipe
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {
                                        //hide progress
                                        progressBar.setVisibility(View.INVISIBLE);

                                        // Handle load complete event if needed

                                        Log.d(TAG, "Load complete, number of pages: " + nbPages);
                                    }
                                })
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {
                                        //book loaded
                                        //hide progress
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "loadComplete: Book Loaded...");
                                    }
                                })
                                .load();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "onSuccess: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    //hide progress
                    progressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onFailure: Failed getting file from URL due to " + e.getMessage());
                });
    }

    public static void loadCategory(String categoryId, TextView categoryTv) {
        //get category using categoryId


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //get category
                        String category = ""+snapshot.child("category").getValue();

                        //set to category textfield
                        categoryTv.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public static void incrementBookViewCount(String bookId){
        //1) get book views count
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("book");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get views count
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        //in case of null replace with 0
                        if (viewsCount.equals("") || viewsCount.equals("null")){
                            viewsCount = "0";
                        }

                        //2) increment views count
                        long newViewsCount = Long.parseLong(viewsCount) + 1;

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("viewsCount", newViewsCount);

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("book");
                        reference.child(bookId)
                                .updateChildren(hashMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}
