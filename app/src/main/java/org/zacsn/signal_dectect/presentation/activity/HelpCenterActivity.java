package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.zacsn.signal_dectect.R;

public class HelpCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_center);

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        // Setup all 9 collapsible cards
        setupCollapsibleCard(findViewById(R.id.ll_header_1), findViewById(R.id.tv_body_1), findViewById(R.id.iv_arrow_1));
        setupCollapsibleCard(findViewById(R.id.ll_header_2), findViewById(R.id.tv_body_2), findViewById(R.id.iv_arrow_2));
        setupCollapsibleCard(findViewById(R.id.ll_header_3), findViewById(R.id.tv_body_3), findViewById(R.id.iv_arrow_3));
        setupCollapsibleCard(findViewById(R.id.ll_header_4), findViewById(R.id.tv_body_4), findViewById(R.id.iv_arrow_4));
        setupCollapsibleCard(findViewById(R.id.ll_header_5), findViewById(R.id.tv_body_5), findViewById(R.id.iv_arrow_5));
        setupCollapsibleCard(findViewById(R.id.ll_header_6), findViewById(R.id.tv_body_6), findViewById(R.id.iv_arrow_6));
        setupCollapsibleCard(findViewById(R.id.ll_header_7), findViewById(R.id.tv_body_7), findViewById(R.id.iv_arrow_7));
        setupCollapsibleCard(findViewById(R.id.ll_header_8), findViewById(R.id.tv_body_8), findViewById(R.id.iv_arrow_8));
        setupCollapsibleCard(findViewById(R.id.ll_header_9), findViewById(R.id.tv_body_9), findViewById(R.id.iv_arrow_9));
    }

    private void setupCollapsibleCard(View headerView, View bodyView, View arrowView) {
        if (headerView == null || bodyView == null || arrowView == null) return;
        bodyView.setVisibility(View.GONE);
        headerView.setOnClickListener(v -> {
            boolean isVisible = bodyView.getVisibility() == View.VISIBLE;
            if (isVisible) {
                bodyView.setVisibility(View.GONE);
                arrowView.animate().rotation(0).setDuration(200).start();
            } else {
                bodyView.setVisibility(View.VISIBLE);
                arrowView.animate().rotation(90).setDuration(200).start();
            }
        });
    }
}
