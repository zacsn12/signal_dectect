package org.zacsn.signal_dectect.presentation.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.data.scan.LanScanner;
import org.zacsn.signal_dectect.domain.model.LanDevice;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 局域网扫描 ViewModel。
 *
 * 发现逻辑已下沉到 {@link LanScanner}（协程 + 主动端口探测）。本类仅负责把引擎的
 * 结果桥接到 LiveData，并保持原有对外契约（getScanState / getDevices / getStatusText /
 * startScan / stopScan / LanScanState）不变，因此界面层无需改动。
 */
@HiltViewModel
public class LanScanViewModel extends ViewModel {

    private final MutableLiveData<LanScanState> scanState = new MutableLiveData<>(LanScanState.IDLE);
    private final MutableLiveData<List<LanDevice>> devices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusText = new MutableLiveData<>("准备扫描");

    private final LanScanner scanner;

    @Inject
    public LanScanViewModel(@ApplicationContext Context context) {
        scanner = new LanScanner(context);
        scanner.setListener(new LanScanner.Listener() {
            @Override
            public void onDevices(List<LanDevice> list) {
                devices.postValue(list);
            }

            @Override
            public void onStatus(String text) {
                statusText.postValue(text);
            }

            @Override
            public void onState(LanScanner.State state) {
                switch (state) {
                    case SCANNING:
                        scanState.postValue(LanScanState.SCANNING);
                        break;
                    case COMPLETE:
                        scanState.postValue(LanScanState.COMPLETE);
                        break;
                    case ERROR:
                        scanState.postValue(LanScanState.ERROR);
                        break;
                    case IDLE:
                    default:
                        scanState.postValue(LanScanState.IDLE);
                        break;
                }
            }
        });
    }

    public void startScan() {
        scanner.start();
    }

    public void stopScan() {
        scanner.stop();
        scanState.postValue(LanScanState.IDLE);
        statusText.postValue("扫描已停止");
    }

    public LiveData<LanScanState> getScanState() {
        return scanState;
    }

    public LiveData<List<LanDevice>> getDevices() {
        return devices;
    }

    public LiveData<String> getStatusText() {
        return statusText;
    }

    @Override
    protected void onCleared() {
        scanner.shutdown();
        super.onCleared();
    }

    public enum LanScanState {
        IDLE, SCANNING, COMPLETE, ERROR
    }
}
