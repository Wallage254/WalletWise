package com.example.wisewallet;
public class Budget {
    private String category;
    private String subCategory;
    private String amount;
    private String date;

    public Budget() {
        // Needed for Firebase
    }

    public Budget(String category, String subCategory, String amount, String date) {
        this.category = category;
        this.subCategory = subCategory;
        this.amount = amount;
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public String getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    public void setAmount(String s) {
    }
}
