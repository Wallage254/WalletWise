package com.example.wisewallet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wallet_wise.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<TransactionModel> transactionList;

    public TransactionAdapter(List<TransactionModel> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_list, parent, false);
        return new TransactionViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Context context = holder.itemView.getContext();

        TransactionModel model = transactionList.get(position);

        holder.amount.setText("Sh " + model.getAmount());
        holder.date.setText(model.getDate());
        holder.note.setText(model.getNote());
        holder.category.setText(model.getCategory());
        holder.time.setText(model.getTime());

        if ("Income".equalsIgnoreCase(model.getType())) {
            holder.amount.setTextColor(Color.parseColor("#4CAF50")); // green
        } else {
            holder.amount.setTextColor(Color.parseColor("#F44336")); // red
        }

        holder.itemView.setOnLongClickListener(v -> {
            showActionDialog(context, transactionList.get(holder.getAdapterPosition()));
            return true;
        });
    }

    private void showActionDialog(Context context, TransactionModel model) {
        String[] options = {"Update", "Delete"};

        new AlertDialog.Builder(context)
                .setTitle("Choose Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Update
                        Intent intent = new Intent(context, ActivityAddTransaction.class);
                        intent.putExtra("transactionId", model.getId()); // Make sure model has an ID
                        intent.putExtra("type", model.getType());
                        intent.putExtra("amount", model.getAmount());
                        intent.putExtra("date", model.getDate());
                        intent.putExtra("note", model.getNote());
                        intent.putExtra("category", model.getCategory());
                        intent.putExtra("subcategory", model.getSubcategory());
                        intent.putExtra("paymentMethod", model.getPaymentMethod());
                        context.startActivity(intent);

                    } else { // Delete
                        new AlertDialog.Builder(context)
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this transaction?")
                                .setPositiveButton("Delete", (d, w) -> {
                                    FirebaseDatabase.getInstance()
                                            .getReference("transactions")
                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .child(model.getId())
                                            .removeValue()
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(context, "Transaction Deleted", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }).show();
    }


    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView amount, date, note, category, time;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.amount);
            date = itemView.findViewById(R.id.date);
            note = itemView.findViewById(R.id.description);
            category = itemView.findViewById(R.id.category);
            time = itemView.findViewById(R.id.time);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<TransactionModel> newList) {
        this.transactionList.clear();
        this.transactionList.addAll(newList);
        notifyDataSetChanged();
    }
}
