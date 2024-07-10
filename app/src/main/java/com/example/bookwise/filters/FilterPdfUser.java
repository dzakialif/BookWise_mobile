package com.example.bookwise.filters;

import android.widget.Filter;

import com.example.bookwise.adapters.AdapterPdfUser;
import com.example.bookwise.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfUser extends Filter {

    //array list in which to search
    ArrayList<ModelPdf> filterList;
    //adapter in which filter need to be implemented
    AdapterPdfUser adapterPdfUser;

    //contructor
    public FilterPdfUser(ArrayList<ModelPdf> filterList, AdapterPdfUser adapterPdfUser) {
        this.filterList = filterList;
        this.adapterPdfUser = adapterPdfUser;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //value to be searched should not be null/empty
        if (constraint != null || constraint.length() > 0){
            //not null or empty
            //change to uppercase or lowercase to avoid case sensitivity
            constraint = constraint.toString().toUpperCase();
            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++){
                //validate
                if (filterList.get(i).getTitle().toUpperCase().contains(constraint)){
                    //search matches, add to list
                    filteredModels.add(filteredModels.get(i));
                }
            }

            results.count = filteredModels.size();
            results.values = filteredModels;
        }
        else {
            //empty or null, make original list/result
            results.count = filterList.size();
            results.values = filterList;
        }
        return results; //dont miss it
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        //apply filter changes
        adapterPdfUser.pdfArrayList = (ArrayList<ModelPdf>) results.values;

        //notify
        adapterPdfUser.notifyDataSetChanged();
    }
}
