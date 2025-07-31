package com.example.wisewallet;

public class TransactionModel {
    private String amount;
    private String date;
    private String note;
    private String recurrence;
    private String category;
    private String time;
    private String type;
    private String name;
    private String lastProcessedDate;
    private String subCategory;
    String paymentMethod;
    private String id;

    public TransactionModel() {}

    public TransactionModel(String amount, String date, String note, String recurrence,
                            String category, String time, String type, String name,
                            String subCategory, String lastProcessedDate, String paymentMethod, String id) {
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.recurrence = recurrence;
        this.category = category;
        this.time = time;
        this.type = type;
        this.name = name;
        this.subCategory = subCategory;
        this.lastProcessedDate = lastProcessedDate;
        this.paymentMethod = paymentMethod;
        this.id = id;
    }

    public TransactionModel(String item, String category, String subcategory, String amount ) {
        this.name = item;
        this.category = category;
        this.subCategory = subcategory;
        this.amount = amount;
    }

    public String getName() { return name; }
    public String getSubcategory() { return subCategory; }

    public String getAmount() { return amount; }
    public String getDate() { return date; }
    public String getNote() { return note; }
    public String getRecurrence() { return recurrence; }
    public String getCategory() { return category; }
    public String getTime() { return time; }
    public String getType() { return type; }
    public String getId() { return id; }

    public void setAmount(String amount) { this.amount = amount; }
    public void setDate(String date) { this.date = date; }
    public void setNote(String note) { this.note = note; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }
    public void setCategory(String category) { this.category = category; }
    public void setTime(String time) { this.time = time; }
    public void setType(String type) { this.type = type; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getLastProcessedDate() { return lastProcessedDate; }
    public void setLastProcessedDate(String lastProcessedDate) { this.lastProcessedDate = lastProcessedDate; }

    public void setSubcategory(String subcategory) {
    }
    public void setId(String id) { this.id = id; }
}
