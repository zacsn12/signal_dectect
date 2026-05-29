package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.database.BlacklistDao;
import org.zacsn.signal_dectect.data.database.BlacklistItemEntity;
import org.zacsn.signal_dectect.data.database.WatchlistDao;
import org.zacsn.signal_dectect.data.database.WatchlistItemEntity;
import org.zacsn.signal_dectect.data.database.WhitelistDao;
import org.zacsn.signal_dectect.data.database.WhitelistItemEntity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ListManagerActivity extends AppCompatActivity {

    public static final String EXTRA_LIST_TYPE = "extra_list_type";
    public static final int TYPE_WATCHLIST = 1;
    public static final int TYPE_WHITELIST = 2;
    public static final int TYPE_BLACKLIST = 3;

    @Inject
    WatchlistDao watchlistDao;
    @Inject
    WhitelistDao whitelistDao;
    @Inject
    BlacklistDao blacklistDao;

    private int listType;
    private ListManagerAdapter adapter;
    private View emptyStateView;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_manager);

        listType = getIntent().getIntExtra(EXTRA_LIST_TYPE, TYPE_WATCHLIST);

        tvTitle = findViewById(R.id.tv_title);
        emptyStateView = findViewById(R.id.ll_empty_state);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        ImageView ivBack = findViewById(R.id.iv_back);

        ivBack.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListManagerAdapter(this::deleteItem);
        recyclerView.setAdapter(adapter);

        setupTitle();
        observeData();

        fabAdd.setOnClickListener(v -> showAddDialog());
    }

    private void setupTitle() {
        if (listType == TYPE_WATCHLIST) {
            tvTitle.setText("信号巡检机型");
        } else if (listType == TYPE_WHITELIST) {
            tvTitle.setText("白名单管理");
        } else if (listType == TYPE_BLACKLIST) {
            tvTitle.setText("黑名单管理");
        }
    }

    private void observeData() {
        if (listType == TYPE_WATCHLIST) {
            watchlistDao.getAll().observe(this, entities -> {
                List<ListItem> items = new ArrayList<>();
                for (WatchlistItemEntity entity : entities) {
                    items.add(new ListItem(entity.getMacAddress(), entity.getDeviceName()));
                }
                updateList(items);
            });
        } else if (listType == TYPE_WHITELIST) {
            whitelistDao.getAll().observe(this, entities -> {
                List<ListItem> items = new ArrayList<>();
                for (WhitelistItemEntity entity : entities) {
                    items.add(new ListItem(entity.getMacAddress(), entity.getDeviceName()));
                }
                updateList(items);
            });
        } else if (listType == TYPE_BLACKLIST) {
            blacklistDao.getAll().observe(this, entities -> {
                List<ListItem> items = new ArrayList<>();
                for (BlacklistItemEntity entity : entities) {
                    items.add(new ListItem(entity.getMacAddress(), entity.getDeviceName()));
                }
                updateList(items);
            });
        }
    }

    private void updateList(List<ListItem> items) {
        adapter.setItems(items);
        if (items.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void showAddDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_list_item, null);
        EditText etPrimary = view.findViewById(R.id.et_primary);
        EditText etSecondary = view.findViewById(R.id.et_secondary);

        if (listType == TYPE_WATCHLIST) {
            etPrimary.setHint("厂商名称 (如 apple)");
        } else {
            etPrimary.setHint("MAC地址 (如 00:11:22:33:44:55)");
        }

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("添加", (dialog, which) -> {
                    String primary = etPrimary.getText().toString().trim();
                    String secondary = etSecondary.getText().toString().trim();

                    if (primary.isEmpty()) {
                        Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    insertItem(primary, secondary);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void insertItem(String primary, String secondary) {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            if (listType == TYPE_WATCHLIST) {
                watchlistDao.insert(new WatchlistItemEntity(primary, secondary, "all", primary, now));
            } else if (listType == TYPE_WHITELIST) {
                whitelistDao.insert(new WhitelistItemEntity(primary, secondary, "all", "", now));
            } else if (listType == TYPE_BLACKLIST) {
                blacklistDao.insert(new BlacklistItemEntity(primary, secondary, "all", "", now, ""));
            }
        }).start();
    }

    private void deleteItem(ListItem item) {
        new Thread(() -> {
            if (listType == TYPE_WATCHLIST) {
                watchlistDao.delete(item.primaryKey);
            } else if (listType == TYPE_WHITELIST) {
                whitelistDao.delete(item.primaryKey);
            } else if (listType == TYPE_BLACKLIST) {
                blacklistDao.delete(item.primaryKey);
            }
        }).start();
    }
}
