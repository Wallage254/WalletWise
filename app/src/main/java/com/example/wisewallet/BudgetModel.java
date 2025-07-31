package com.example.wisewallet;

public class BudgetModel {
    private String category;
    private String subcategory;
    private String amount;
    private String spent;
    private String remaining;
    private String budgetPeriod;

    public BudgetModel() { }

    public BudgetModel(String category, String subcategory, String amount, String budgetPeriod) {
        this.category = category;
        this.subcategory = subcategory;
        this.amount = amount;
        this.spent = "0";
        this.remaining = amount;
        this.budgetPeriod = budgetPeriod;
    }

    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public String getAmount() { return amount; }
    public String getSpent() { return spent; }
    public String getRemaining() { return remaining; }
    public String getBudgetPeriod() { return budgetPeriod; }

    public void setAmount(String amount) { this.amount = amount; }
    public void setSpent(String spent) { this.spent = spent; }
    public void setRemaining(String remaining) { this.remaining = remaining; }
}
