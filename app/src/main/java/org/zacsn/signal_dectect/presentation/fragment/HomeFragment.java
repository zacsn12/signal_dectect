package org.zacsn.signal_dectect.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.zacsn.signal_dectect.databinding.FragmentHomeBinding;
import org.zacsn.signal_dectect.presentation.activity.LanDeviceScanActivity;
import org.zacsn.signal_dectect.presentation.activity.SignalInspectActivity;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {
    
    private FragmentHomeBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
    }
    
    private void setupClickListeners() {
        // Bluetooth Inspect
        binding.cardBluetoothInspect.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SignalInspectActivity.class);
            intent.putExtra("SCAN_TYPE", "BLUETOOTH");
            startActivity(intent);
        });
        
        // WiFi Inspect
        binding.cardWifiInspect.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SignalInspectActivity.class);
            intent.putExtra("SCAN_TYPE", "WIFI");
            startActivity(intent);
        });
        
        // LAN Scan - Use dedicated LAN scan activity
        binding.cardLanScan.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LanDeviceScanActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
