package com.example.bookwise.filters;

import android.widget.Filter;

import com.example.bookwise.adapters.AdapterPdfUser;
import com.example.bookwise.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfUser extends Filter {

    private ArrayList<ModelPdf> filterList;
    private AdapterPdfUser adapter;

    public FilterPdfUser(ArrayList<ModelPdf> filterList, AdapterPdfUser adapter) {
        this.filterList = filterList;
        this.adapter = adapter;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        if (constraint != null && constraint.length() > 0) {
            constraint = constraint.toString().toLowerCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();
            for (ModelPdf model : filterList) {
                if (model.getTitle().toLowerCase().contains(constraint) ||
                        model.getDescription().toLowerCase().contains(constraint)) {
                    filteredModels.add(model);
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        } else {
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.pdfArrayList = (ArrayList<ModelPdf>) results.values;
        adapter.notifyDataSetChanged();
    }
}
