package com.example.wisewallet;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CategoryData {

    public static Map<String, List<String>> loadCategories(Context context) {
        Map<String, List<String>> categoryMap = new HashMap<>();
        try {
            AssetManager assetManager = context.getAssets();
            InputStream is = assetManager.open("categories.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String category = keys.next();
                JSONArray subcategories = jsonObject.getJSONArray(category);
                List<String> subList = new ArrayList<>();
                for (int i = 0; i < subcategories.length(); i++) {
                    subList.add(subcategories.getString(i));
                }
                categoryMap.put(category, subList);
            }
        } catch (Exception e) {
            Log.e("CategoryData", "Error loading categories", e);
        }
        return categoryMap;
    }


    public static Map<String, List<String>> loadIncomeCategories(Context context) {
        Map<String, List<String>> map = new HashMap<>();
        map.put("Salary", Arrays.asList("Monthly Salary", "Bonus"));
        map.put("Business", Arrays.asList("Sales", "Freelance"));
        map.put("Investments", Arrays.asList("Dividends", "Interest"));
        return map;
    }
}
