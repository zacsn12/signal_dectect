package org.zacsn.signal_dectect;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.widget.Toast;

import org.zacsn.signal_dectect.databinding.ActivityMainBinding;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.presentation.fragment.HomeFragment;
import org.zacsn.signal_dectect.presentation.fragment.RecordsFragment;
import org.zacsn.signal_dectect.util.PermissionManager;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    
    @Inject
    PermissionManager permissionManager;
    
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupPermissionLauncher();
        setupBottomNavigation();
        checkAndRequestPermissions();
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_scan) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.navigation_records) {
                fragment = new RecordsFragment();
            }
            
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    if (!entry.getValue()) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "部分权限被拒绝，某些功能可能无法使用", 
                        Toast.LENGTH_LONG).show();
                }
            }
        );
    }
    
    private void checkAndRequestPermissions() {
        // Check permissions for all scan types
        ScanType allScans = ScanType.ALL;
        
        if (!permissionManager.hasRequiredPermissions(allScans)) {
            String[] missingPermissions = permissionManager.getMissingPermissions(allScans);
            permissionLauncher.launch(missingPermissions);
        }
    }
    
    /**
     * Request permissions for a specific scan type.
     * Can be called from fragments or activities.
     */
    public void requestPermissionsForScan(ScanType scanType) {
        if (!permissionManager.hasRequiredPermissions(scanType)) {
            String[] missingPermissions = permissionManager.getMissingPermissions(scanType);
            permissionLauncher.launch(missingPermissions);
        }
    }
}