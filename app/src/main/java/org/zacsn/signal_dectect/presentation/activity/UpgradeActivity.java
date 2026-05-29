package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.zacsn.signal_dectect.BuildConfig;
import org.zacsn.signal_dectect.R;

public class UpgradeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        ImageView ivBack = findViewById(R.id.iv_back);
        TextView tvVersion = findViewById(R.id.tv_version);
        MaterialButton btnCheckUpdate = findViewById(R.id.btn_check_update);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        ivBack.setOnClickListener(v -> finish());
        
        tvVersion.setText("当前版本: v" + BuildConfig.VERSION_NAME);

        btnCheckUpdate.setOnClickListener(v -> {
            btnCheckUpdate.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            
            // Simulate network delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                progressBar.setVisibility(View.GONE);
                btnCheckUpdate.setEnabled(true);
                Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
            }, 1500);
        });
    }
}
