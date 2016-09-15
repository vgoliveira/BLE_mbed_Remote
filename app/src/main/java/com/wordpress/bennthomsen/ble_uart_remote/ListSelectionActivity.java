package com.wordpress.bennthomsen.ble_uart_remote;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


import java.io.InputStream;
import java.util.ArrayList;

public class ListSelectionActivity extends ListActivity {

    private ArrayList<String> bookTitles;
    private ArrayList<String> recipeTitles;
    private ArrayList<String> bookFiles;
    private ArrayList<String> availableWeights;
    private TextView listTitle;
    private ArrayAdapter<String> listAdapter;
    private XMLPullParserHandler fetcher;
    private String level = "";
    Bundle parameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.list_selection);
        listTitle = (TextView) findViewById(R.id.listTitle);
        parameters = getIntent().getExtras();
        InputStream is;

        level = parameters.getString("level");
        switch(level) {
            case "book":
                listTitle.setText(parameters.getString("list_title"));
                is = getResources().openRawResource(getResources().getIdentifier(parameters.getString("file"),"raw", getPackageName()));
                fetcher = new XMLPullParserHandler(is);
                bookTitles = fetcher.parseForTagList("title");
                bookFiles = fetcher.parseForTagList("filename");
                listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, bookTitles);
                setListAdapter(listAdapter);
                break;
            case "recipe":
                listTitle.setText(parameters.getString("list_title"));
                is = getResources().openRawResource(getResources().getIdentifier(parameters.getString("file"),"raw", getPackageName()));
                fetcher = new XMLPullParserHandler(is);
                recipeTitles = fetcher.parseForTagList("title");
                listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, recipeTitles);
                setListAdapter(listAdapter);
                break;
            case "weight":
                listTitle.setText(parameters.getString("list_title"));
                availableWeights = new ArrayList<>();
                boolean[] weights = parameters.getBooleanArray("available_weights");
                if(weights[Recipe.W450]){
                    availableWeights.add("450 gramas");
                }
                if(weights[Recipe.W600]){
                    availableWeights.add("600 gramas");
                }
                if(weights[Recipe.W900]){
                    availableWeights.add("900 gramas");
                }
                if(weights[Recipe.W1200]){
                    availableWeights.add("1200 gramas");
                }
                listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, availableWeights);
                setListAdapter(listAdapter);

                break;

        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        switch(level) {
            case "book":
                String filename = bookFiles.get(bookTitles.indexOf(getListAdapter().getItem(position).toString()));
                Intent recipeIntent = new Intent(ListSelectionActivity.this, ListSelectionActivity.class);
                recipeIntent.putExtra("file", filename);
                recipeIntent.putExtra("level", "recipe");
                recipeIntent.putExtra ("list_title",getListAdapter().getItem(position).toString());
                startActivityForResult(recipeIntent, MainActivity.REQUEST_SELECT_RECIPE);
                break;
            case "recipe":
                Recipe recipe = fetcher.parseForRecipe(getListAdapter().getItem(position).toString());
                Intent weigthIntent = new Intent(ListSelectionActivity.this, ListSelectionActivity.class);
                weigthIntent.putExtra("level", "weight");
                weigthIntent.putExtra ("list_title",recipe.getTitle()+": escolha o peso");
                weigthIntent.putExtra("available_weights", recipe.getAvailableWeight());
                startActivityForResult(weigthIntent, MainActivity.REQUEST_SELECT_RECIPE);
                break;
            case "weight":
                break;
        }
    }
}
