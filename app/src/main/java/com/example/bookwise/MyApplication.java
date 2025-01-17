package com.example.bookwise;

import static com.example.bookwise.Constants.MAX_BYTES_PDF;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
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
import com.google.firebase.auth.FirebaseAuth;
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

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";
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

    public static void loadPdfFromUrlSinglePage(String pdfUrl, String bookTitle, PDFView pdfView, ProgressBar progressBar, TextView pagesTv) {
        // Using URL to get the file and its metadata from Firebase Storage
        String TAG = "BOOK_LOAD_SINGLE_TAG";

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);

        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(bytes -> {
                    Log.d(TAG, "onSuccess: " + bookTitle + " Successfully got the file");

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
                                        // hide progress
                                        progressBar.setVisibility(View.INVISIBLE);

                                        // Handle load complete event if needed
                                        Log.d(TAG, "Load complete, number of pages: " + nbPages);

                                        // if pagesTv param is not null then set page numbers
                                        if (pagesTv != null) {
                                            pagesTv.setText(String.valueOf(nbPages)); // concatenate with double quotes because can't set int in textview
                                        }
                                    }
                                })
                                .load();
                    } catch (IOException e) {
                        e.printStackTrace();
                        progressBar.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onSuccess: Exception while writing file: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    // hide progress
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

//    public static void downloadBook(Context context, String bookId, String bookTitle, String bookUrl){
//        Log.d(TAG_DOWNLOAD, "downloadBook: downloading book...");
//        String nameWithExtension = bookTitle + ".pdf";
//        Log.d(TAG_DOWNLOAD, "downloadBook: NAME : "+nameWithExtension);
//
//        //progress dialog
//        ProgressDialog progressDialog = new ProgressDialog(context);
//        progressDialog.setTitle("Please Wait");
//        progressDialog.setMessage("Downloading "+ nameWithExtension + "...");
//        progressDialog.setCanceledOnTouchOutside(false);
//        progressDialog.show();
//
//        //download from firebase storage using url
//        StorageReference storageReference = FirebaseStorage.getInstance().getReference(bookUrl);
//        storageReference.getBytes(MAX_BYTES_PDF)
//                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                    @Override
//                    public void onSuccess(byte[] bytes) {
//                        Log.d(TAG_DOWNLOAD, "onSuccess: Book Downloaded");
//                        Log.d(TAG_DOWNLOAD, "onSuccess: Saving book...");
//                        saveDownloadedBook(context, progressDialog, bytes, nameWithExtension, bookId);
//
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.d(TAG_DOWNLOAD, "onFailure: Failed to download due to "+e.getMessage());
//                        progressDialog.dismiss();
//                        Toast.makeText(context, "Failed to download due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//    }
//
//    private static void saveDownloadedBook(Context context, ProgressDialog progressDialog, byte[] bytes, String nameWithExtension, String bookId) {
//        Log.d(TAG_DOWNLOAD, "saveDownloadedBook: Saving downloaded book");
//        try {
//            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            downloadsFolder.mkdirs();
//
//            String filePath = downloadsFolder.getPath() + "/" + nameWithExtension;
//
//            FileOutputStream out = new FileOutputStream(filePath);
//            out.write(bytes);
//            out.close();
//
//            Toast.makeText(context, "Saved to Download Folder", Toast.LENGTH_SHORT).show();
//            Log.d(TAG_DOWNLOAD, "saveDownloadedBook: Saved to Download Folder");
//            progressDialog.dismiss();
//
//            incrementBookDownloadCount(bookId);
//        }
//        catch (Exception e){
//            Log.d(TAG_DOWNLOAD, "saveDownloadedBook: Failed saving to Download Folder due to "+e.getMessage());
//            Toast.makeText(context, "Failed saving to Download Folder due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
//            progressDialog.dismiss();
//
//        }
//    }
//
//    private static void incrementBookDownloadCount(String bookId) {
//        Log.d(TAG_DOWNLOAD, "incrementBookDownloadCount: Incrementing Book Download Count");
//
//        //Step 1 Get previous download count
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("book");
//        ref.child(bookId)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
//                        Log.d(TAG_DOWNLOAD, "onDataChange: Download Count: "+downloadsCount);
//
//                        if (downloadsCount.equals("") || downloadsCount.equals(null)){
//                            downloadsCount = "0";
//                        }
//
//                        //convert to long and increment 1
//                        long newDownloadsCount = Long.parseLong(downloadsCount) + 1;
//                        Log.d(TAG_DOWNLOAD, "onDataChange: New Download Count: "+newDownloadsCount);
//
//                        //setup data to update
//                        HashMap<String, Object> hashMap = new HashMap<>();
//                        hashMap.put("downloadsCount", newDownloadsCount);
//
//                        //Step 2
//                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("book");
//                        reference.child(bookId).updateChildren(hashMap)
//                                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                    @Override
//                                    public void onSuccess(Void unused) {
//                                        Log.d(TAG_DOWNLOAD, "onSuccess: Download Count Updated...");
//                                    }
//                                })
//                                .addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Log.d(TAG_DOWNLOAD, "onFailure: Failed to update Download Count due to"+e.getMessage());
//                                    }
//                                });
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });
//    }

    public static void addToFavorite(Context context, String bookId){
        //we can add only if user is logged in
        //1)check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null){
            //not logged in, cant add to fav
            Toast.makeText(context, "You're Not Logged In", Toast.LENGTH_SHORT).show();
        }
        else {
            long timestamp = System.currentTimeMillis();

            //setup data to add in firebase db of current user for favorite book
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("bookId", "" + bookId);
            hashMap.put("timestamp", "" + timestamp);

            //save to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");
            ref.child(firebaseAuth.getUid()).child("favorites").child(bookId)
                    .setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Added to your Favorite list...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to add to your Favorite list due to " + e.getMessage() , Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public static void removeFromFavorite(Context context, String bookId){
        //we can remove only if user is logged in
        //1)check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null){
            //not logged in, cant remove from fav
            Toast.makeText(context, "You're Not Logged In", Toast.LENGTH_SHORT).show();
        }
        else {

            //remove from db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("user");
            ref.child(firebaseAuth.getUid()).child("favorites").child(bookId)
                    .removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Removed from your Favorite list...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to remove from your Favorite list due to " + e.getMessage() , Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}
