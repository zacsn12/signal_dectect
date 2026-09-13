package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.databinding.ActivityFakeBaseStationBinding;
import org.zacsn.signal_dectect.domain.model.FakeBaseStationCell;
import org.zacsn.signal_dectect.domain.model.FakeCellRiskLevel;
import org.zacsn.signal_dectect.presentation.adapter.FakeBaseStationAdapter;
import org.zacsn.signal_dectect.presentation.viewmodel.FakeBaseStationViewModel;
import org.zacsn.signal_dectect.util.SoundEffectManager;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FakeBaseStationActivity extends AppCompatActivity {

    private ActivityFakeBaseStationBinding binding;
    private FakeBaseStationViewModel viewModel;
    private FakeBaseStationAdapter adapter;
    private SoundEffectManager soundEffectManager;
    private boolean isScanning = false;
    private AlertDialog activeAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFakeBaseStationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        soundEffectManager = new SoundEffectManager(this);
        viewModel = new ViewModelProvider(this).get(FakeBaseStationViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupScanButton();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new FakeBaseStationAdapter(this::showCellDetailDialog);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupScanButton() {
        binding.btnStartScan.setOnClickListener(v -> {
            if (isScanning) {
                viewModel.stopScan();
            } else {
                viewModel.startScan();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getScanState().observe(this, state -> {
            isScanning = state == FakeBaseStationViewModel.ScanState.SCANNING;
            if (isScanning) {
                binding.btnStartScan.setText("停止排查");
                binding.btnStartScan.setIconResource(android.R.drawable.ic_media_pause);
                binding.progressBar.setVisibility(View.VISIBLE);

                Animation rotateAnim = AnimationUtils.loadAnimation(this, R.anim.radar_sweep);
                binding.radarSweep.startAnimation(rotateAnim);
                soundEffectManager.startNormalScanSound();
            } else {
                binding.btnStartScan.setText("开始排查");
                binding.btnStartScan.setIconResource(android.R.drawable.ic_media_play);
                binding.progressBar.setVisibility(View.GONE);
                binding.radarSweep.clearAnimation();
                soundEffectManager.stopAllSounds();
            }
        });

        viewModel.getCells().observe(this, cells -> {
            adapter.submitList(cells);
            binding.tvCellCount.setText("监测中: " + cells.size());
            binding.layoutEmptyState.setVisibility(cells.isEmpty() && !isScanning ? View.VISIBLE : View.GONE);
        });

        viewModel.getOverallRiskLevel().observe(this, this::updateSecurityHeader);

        viewModel.getStatusText().observe(this, status -> binding.tvStatus.setText(status));

        viewModel.getThreatAlertEvent().observe(this, threatCell -> {
            if (threatCell != null) {
                handleThreatDetected(threatCell);
            }
        });
    }

    private void updateSecurityHeader(FakeCellRiskLevel riskLevel) {
        if (riskLevel == FakeCellRiskLevel.DANGEROUS) {
            binding.ivSecurityIcon.setImageResource(R.drawable.ic_alert);
            binding.ivSecurityIcon.setColorFilter(0xFFEF4444);
            binding.tvSecurityTitle.setText("⚠️ 发现伪基站威胁");
            binding.tvSecurityTitle.setTextColor(0xFFFCA5A5);
            binding.tvSecuritySubtitle.setText("已监测到高危伪基站信令特征！");
            binding.tvSecuritySubtitle.setTextColor(0xFFFEE2E2);
        } else if (riskLevel == FakeCellRiskLevel.SUSPICIOUS) {
            binding.ivSecurityIcon.setImageResource(R.drawable.ic_shield_check);
            binding.ivSecurityIcon.setColorFilter(0xFFF59E0B);
            binding.tvSecurityTitle.setText("环境可疑");
            binding.tvSecurityTitle.setTextColor(0xFFFDE68A);
            binding.tvSecuritySubtitle.setText("存在弱异常基站特征，请注意防范");
            binding.tvSecuritySubtitle.setTextColor(0xFFFEF3C7);
        } else {
            binding.ivSecurityIcon.setImageResource(R.drawable.ic_shield_check);
            binding.ivSecurityIcon.setColorFilter(0xFF10B981);
            binding.tvSecurityTitle.setText("环境安全");
            binding.tvSecurityTitle.setTextColor(0xFFFFFFFF);
            binding.tvSecuritySubtitle.setText("未发现伪基站威胁");
            binding.tvSecuritySubtitle.setTextColor(0xFF93C5FD);
        }
    }

    private void handleThreatDetected(FakeBaseStationCell threatCell) {
        // Trigger alert audio
        soundEffectManager.switchToAlertSound();

        if (activeAlertDialog != null && activeAlertDialog.isShowing()) {
            return;
        }

        StringBuilder reasons = new StringBuilder();
        for (String r : threatCell.getAnomalyReasons()) {
            reasons.append("• ").append(r).append("\n");
        }

        String msg = "【疑似伪基站特征告警】\n\n"
                + "运营商: " + threatCell.getOperator() + "\n"
                + "网络制式: " + threatCell.getRadioType() + "\n"
                + "位置区码 (LAC/TAC): " + threatCell.getLacTac() + "\n"
                + "小区标识 (Cell ID): " + threatCell.getCellId() + "\n"
                + "信号电平: " + threatCell.getSignalDbm() + " dBm\n\n"
                + "触发威胁原因:\n" + reasons.toString().trim();

        activeAlertDialog = new AlertDialog.Builder(this)
                .setTitle("⚠️ 伪基站安全警报")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("我知道了 (静音)", (dialog, which) -> {
                    soundEffectManager.stopAllSounds();
                    viewModel.clearThreatAlertEvent();
                })
                .setNegativeButton("查看详情", (dialog, which) -> {
                    soundEffectManager.stopAllSounds();
                    viewModel.clearThreatAlertEvent();
                    showCellDetailDialog(threatCell);
                })
                .show();
    }

    private void showCellDetailDialog(FakeBaseStationCell cell) {
        StringBuilder sb = new StringBuilder();
        sb.append("运营商: ").append(cell.getOperator()).append("\n");
        sb.append("PLMN代码: ").append(cell.getPlmn()).append("\n");
        sb.append("网络制式: ").append(cell.getRadioType()).append("\n");
        sb.append("服务状态: ").append(cell.isServing() ? "当前主服小区" : "邻区基站").append("\n");
        sb.append("小区标识 (CID): ").append(cell.getCellId()).append("\n");
        sb.append("位置/跟踪区码 (LAC/TAC): ").append(cell.getLacTac()).append("\n");
        sb.append("物理标识 (PCI/PSC): ").append(cell.getPciPsc()).append("\n");
        sb.append("载波频点 (Channel): ").append(cell.getChannel()).append("\n");
        sb.append("接收电平: ").append(cell.getSignalDbm()).append(" dBm\n");
        sb.append("安全评级: ").append(cell.getRiskLevel() == FakeCellRiskLevel.DANGEROUS ? "⚠️ 伪基站威胁" : (cell.getRiskLevel() == FakeCellRiskLevel.SUSPICIOUS ? "可疑关注" : "正常安全")).append("\n");
        sb.append("风险评估分: ").append(cell.getRiskScore()).append(" / 100\n\n");

        if (!cell.getAnomalyReasons().isEmpty()) {
            sb.append("【检测到的异常特征】:\n");
            for (String reason : cell.getAnomalyReasons()) {
                sb.append("• ").append(reason).append("\n");
            }
        } else {
            sb.append("【特征结论】: 未发现伪基站异常特征，基站符合正规运营商规范。");
        }

        new AlertDialog.Builder(this)
                .setTitle("基站射频与安全分析")
                .setMessage(sb.toString().trim())
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isScanning) {
            viewModel.stopScan();
        }
        if (soundEffectManager != null) {
            soundEffectManager.stopAllSounds();
        }
    }

    @Override
    protected void onDestroy() {
        if (soundEffectManager != null) {
            soundEffectManager.release();
            soundEffectManager = null;
        }
        super.onDestroy();
        binding = null;
    }
}
