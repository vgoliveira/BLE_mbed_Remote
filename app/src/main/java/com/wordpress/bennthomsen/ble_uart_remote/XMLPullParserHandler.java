package com.wordpress.bennthomsen.ble_uart_remote;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by vgarcia on 12/09/2016.
 */
public class XMLPullParserHandler {

    private XmlPullParser parser;

    XMLPullParserHandler(String fileName) {
        XmlPullParserFactory factory = null;
        try {
            factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();

            File file = new File("/raw/" + fileName);
            FileInputStream fis = new FileInputStream(file);

            parser.setInput(fis, null);

        } catch (XmlPullParserException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Seek the recipe by the recipeTitle and retruns the recipe object filled with this recipe
    public Recipe parseForRecipe(String recipeTitle) {

        Recipe recipe = new Recipe();
        Ingredient ingredient = null;
        String tagName;
        int eventType;
        String text = "";

        try {
            eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("title")) {
                            if (text.equals(recipeTitle)) {
                                recipe.setTitle(text);
                            } else {
                                while (!parser.getName().equalsIgnoreCase("recipe")) { //go to the end of this recipe
                                    parser.next();
                                }
                            }
                        } else if (tagName.equalsIgnoreCase("id")) {
                            recipe.setId(text);
                        } else if (tagName.equalsIgnoreCase("option")) {
                            recipe.setOption(text);
                        } else if (tagName.equalsIgnoreCase("weight")) {
                            recipe.setAvailableWeight(text);
                        } else if (parser.getName().equalsIgnoreCase("name")) {
                            ingredient = new Ingredient(text);
                        } else if (parser.getName().equalsIgnoreCase("W450") || parser.getName().equalsIgnoreCase("W600") || parser.getName().equalsIgnoreCase("W900") || parser.getName().equalsIgnoreCase("W1200") ) {
                            if(ingredient != null) {
                                ingredient.setMeasure(parser.getName(), text);
                            }
                        } else if (parser.getName().equalsIgnoreCase("ingredient")) {
                            recipe.addIngredient(ingredient);
                        } else if (parser.getName().equalsIgnoreCase("recipe")) {
                            return recipe;
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    ArrayList<String> parseForTitleList () {
        ArrayList<String> titleList = new ArrayList<>();
        String tagName;
        int eventType;
        String text = "";

        try {
            eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("title")) {
                            titleList.add(text);
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return  titleList;
    }
}
