package org.zacsn.signal_dectect;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.widget.Toast;

import org.zacsn.signal_dectect.data.api.AuthApiConfig;
import org.zacsn.signal_dectect.data.api.AuthApiService;
import org.zacsn.signal_dectect.data.api.LoginResponse;
import org.zacsn.signal_dectect.databinding.ActivityMainBinding;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.presentation.fragment.HomeFragment;
import org.zacsn.signal_dectect.presentation.fragment.RecordsFragment;
import org.zacsn.signal_dectect.util.PermissionManager;
import org.zacsn.signal_dectect.util.SessionManager;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    
    @Inject
    PermissionManager permissionManager;
    
    private ActivityResultLauncher<String[]> permissionLauncher;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupPermissionLauncher();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        validateCurrentSession(savedInstanceState);
    }

    private void validateCurrentSession(Bundle savedInstanceState) {
        String token = sessionManager.getToken();
        if (token == null || token.trim().isEmpty()) {
            sessionManager.logout();
            Toast.makeText(this, "登录已失效，请重新登录", Toast.LENGTH_LONG).show();
            goToLogin();
            return;
        }

        AuthApiService apiService = AuthApiConfig.createService();
        apiService.currentUser("Bearer " + token).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                LoginResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Toast.makeText(MainActivity.this, "授权同步异常，暂时使用本地登录状态", Toast.LENGTH_LONG).show();
                    initializeMainScreen(savedInstanceState);
                    return;
                }

                if (body.getCode() == 200) {
                    LoginResponse.Data data = body.getData();
                    if (data != null) {
                        sessionManager.updateAuthorizationInfo(
                                data.getUserId(),
                                data.getNickname(),
                                data.getValidUntil(),
                                data.getMachineCode(),
                                data.getMaxMachineBindings()
                        );
                    }
                    initializeMainScreen(savedInstanceState);
                    return;
                }

                if (body.getCode() == 401 || body.getCode() == 403 || body.getCode() == 404) {
                    sessionManager.logout();
                    Toast.makeText(MainActivity.this, body.getMessage(), Toast.LENGTH_LONG).show();
                    goToLogin();
                    return;
                }

                Toast.makeText(MainActivity.this, body.getMessage(), Toast.LENGTH_LONG).show();
                initializeMainScreen(savedInstanceState);
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                Toast.makeText(MainActivity.this, "授权同步失败，暂时使用本地登录状态", Toast.LENGTH_LONG).show();
                initializeMainScreen(savedInstanceState);
            }
        });
    }

    private void initializeMainScreen(Bundle savedInstanceState) {
        if (binding != null) {
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupBottomNavigation();
        checkAndRequestPermissions();
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    private void goToLogin() {
        android.content.Intent intent = new android.content.Intent(this, org.zacsn.signal_dectect.presentation.activity.LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_scan) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.navigation_records) {
                fragment = new RecordsFragment();
            } else if (itemId == R.id.navigation_profile) {
                fragment = new org.zacsn.signal_dectect.presentation.fragment.ProfileFragment();
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
            if (permissionLauncher != null) {
                permissionLauncher.launch(missingPermissions);
            }
        }
    }
    
    /**
     * Request permissions for a specific scan type.
     * Can be called from fragments or activities.
     */
    public void requestPermissionsForScan(ScanType scanType) {
        if (!permissionManager.hasRequiredPermissions(scanType)) {
            String[] missingPermissions = permissionManager.getMissingPermissions(scanType);
            if (permissionLauncher != null) {
                permissionLauncher.launch(missingPermissions);
            }
        }
    }
}
