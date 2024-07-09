package com.example.bookwise.filters;

import android.widget.Filter;

import com.example.bookwise.adapters.AdapterCategory;
import com.example.bookwise.adapters.AdapterPdfAdmin;
import com.example.bookwise.models.ModelCategory;
import com.example.bookwise.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfAdmin extends Filter {
    //array list in which we want to search
    ArrayList<ModelPdf> filterList;
    //adapter in which filter need to be implemented
    AdapterPdfAdmin adapterPdfAdmin;

    //constructor
    public FilterPdfAdmin(ArrayList<ModelPdf> filterList, AdapterPdfAdmin adapterPdfAdmin) {
        this.filterList = filterList;
        this.adapterPdfAdmin = adapterPdfAdmin;
    }


    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //value should not be null and empty
        if (constraint != null && constraint.length() > 0){

            //change to upper case, or lowercase to avoid case sensitivity
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++){
                //validate
                if (filterList.get(i).getTitle().toUpperCase().contains(constraint)){
                    //add to filtered list
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count = filteredModels.size();
            results.values = filteredModels;
        }
        else {
            results.count = filterList.size();
            results.values = filterList;
        }

        return results; //dont miss it
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        //apply filter changes
        adapterPdfAdmin.pdfArrayList = (ArrayList<ModelPdf>) results.values;

        //notify changes
        adapterPdfAdmin.notifyDataSetChanged();
    }
}
