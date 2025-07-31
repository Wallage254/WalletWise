package com.example.wisewallet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wallet_wise.R;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private final List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems != null ? onboardingItems.size() : 0;
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageOnboarding;
        private final TextView textTitle, textDescription;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            imageOnboarding = itemView.findViewById(R.id.imageOnboarding);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
        }

        void bind(OnboardingItem item) {
            if (item != null) {
                imageOnboarding.setImageResource(item.getImageResId());
                textTitle.setText(item.getTitle());
                textDescription.setText(item.getDescription());

                // Optional: Fade-in animation
                itemView.setAlpha(0f);
                itemView.animate().alpha(1f).setDuration(400).start();
            }
        }
    }
}
