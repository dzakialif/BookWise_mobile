package com.example.bookwise.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookwise.databinding.RowPdfUserBinding;
import com.example.bookwise.models.ModelPdf;

import java.util.ArrayList;

public class AdapterPdfUser extends RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser> {

    private Context context;
    public ArrayList<ModelPdf> pdfArrayList;

    private RowPdfUserBinding binding;

    private static final String TAG = "ADAPTER_PDF_USER_TAG";

    public AdapterPdfUser(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfUser onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull HolderPdfUser holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class HolderPdfUser extends RecyclerView.ViewHolder{

        public HolderPdfUser(@NonNull View itemView) {
            super(itemView);
        }
    }
}
