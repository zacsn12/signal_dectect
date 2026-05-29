package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.database.WatchlistDao;
import org.zacsn.signal_dectect.data.database.WatchlistItemEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DeviceModelActivity extends AppCompatActivity {

    @Inject
    WatchlistDao watchlistDao;

    private DeviceModelAdapter adapter;

    // List of common manufacturers
    private final List<String> COMMON_MANUFACTURERS = Arrays.asList(
            "apple", "samsung", "huawei", "xiaomi", "oppo", "vivo", "honor", "oneplus", "meizu", "sony", "google"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_model);

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DeviceModelAdapter((item, isChecked) -> {
            new Thread(() -> {
                if (isChecked) {
                    watchlistDao.insert(new WatchlistItemEntity(item.name, item.name, "all", item.name, System.currentTimeMillis()));
                } else {
                    watchlistDao.delete(item.name);
                }
            }).start();
        });
        recyclerView.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        watchlistDao.getAll().observe(this, entities -> {
            List<String> savedNames = new ArrayList<>();
            for (WatchlistItemEntity entity : entities) {
                savedNames.add(entity.getMacAddress().toLowerCase());
            }

            List<DeviceModelAdapter.ModelItem> items = new ArrayList<>();
            for (String manufacturer : COMMON_MANUFACTURERS) {
                items.add(new DeviceModelAdapter.ModelItem(manufacturer, savedNames.contains(manufacturer)));
            }

            // Also add any custom ones that are in the database but not in the common list
            for (WatchlistItemEntity entity : entities) {
                String savedName = entity.getMacAddress().toLowerCase();
                if (!COMMON_MANUFACTURERS.contains(savedName)) {
                    items.add(new DeviceModelAdapter.ModelItem(savedName, true));
                }
            }

            adapter.setItems(items);
        });
    }
}
