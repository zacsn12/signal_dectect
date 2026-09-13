package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.data.scanner.FakeBaseStationDetector;
import org.zacsn.signal_dectect.domain.model.FakeBaseStationCell;
import org.zacsn.signal_dectect.domain.model.FakeCellRiskLevel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FakeBaseStationViewModel extends ViewModel {

    public enum ScanState {
        IDLE, SCANNING, ERROR
    }

    private final MutableLiveData<ScanState> scanState = new MutableLiveData<>(ScanState.IDLE);
    private final MutableLiveData<List<FakeBaseStationCell>> cells = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<FakeCellRiskLevel> overallRiskLevel = new MutableLiveData<>(FakeCellRiskLevel.SAFE);
    private final MutableLiveData<Integer> detectedFakeCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> statusText = new MutableLiveData<>("准备开始伪基站排查");
    private final MutableLiveData<FakeBaseStationCell> threatAlertEvent = new MutableLiveData<>(null);

    private final FakeBaseStationDetector detector;
    private boolean hasTriggeredAlertForCurrentRun = false;

    @Inject
    public FakeBaseStationViewModel(FakeBaseStationDetector detector) {
        this.detector = detector;
        this.detector.setListener(new FakeBaseStationDetector.Listener() {
            @Override
            public void onCellsEvaluated(List<FakeBaseStationCell> list, FakeCellRiskLevel overallRisk, boolean hasNewThreat) {
                cells.postValue(list);
                overallRiskLevel.postValue(overallRisk);

                int fakeCount = 0;
                FakeBaseStationCell firstThreat = null;
                for (FakeBaseStationCell cell : list) {
                    if (cell.getRiskLevel() == FakeCellRiskLevel.DANGEROUS) {
                        fakeCount++;
                        if (firstThreat == null) firstThreat = cell;
                    }
                }
                detectedFakeCount.postValue(fakeCount);

                if (fakeCount > 0) {
                    statusText.postValue("⚠️ 警告：检测到 " + fakeCount + " 个疑似伪基站威胁！");
                    if (!hasTriggeredAlertForCurrentRun && firstThreat != null) {
                        hasTriggeredAlertForCurrentRun = true;
                        threatAlertEvent.postValue(firstThreat);
                    }
                } else if (overallRisk == FakeCellRiskLevel.SUSPICIOUS) {
                    statusText.postValue("提示：当前环境存在弱异常基站，已列出特征");
                } else {
                    statusText.postValue("环境安全 · 监测到 " + list.size() + " 个合规基站");
                }
            }

            @Override
            public void onStatus(String status) {
                statusText.postValue(status);
            }

            @Override
            public void onError(String error) {
                scanState.postValue(ScanState.ERROR);
                statusText.postValue("扫描错误: " + error);
            }
        });
    }

    public void startScan() {
        hasTriggeredAlertForCurrentRun = false;
        scanState.postValue(ScanState.SCANNING);
        statusText.postValue("正在扫描基站并分析射频与信令特征...");
        detector.start();
    }

    public void stopScan() {
        detector.stop();
        scanState.postValue(ScanState.IDLE);
        statusText.postValue("伪基站扫描已停止");
    }

    public LiveData<ScanState> getScanState() {
        return scanState;
    }

    public LiveData<List<FakeBaseStationCell>> getCells() {
        return cells;
    }

    public LiveData<FakeCellRiskLevel> getOverallRiskLevel() {
        return overallRiskLevel;
    }

    public LiveData<Integer> getDetectedFakeCount() {
        return detectedFakeCount;
    }

    public LiveData<String> getStatusText() {
        return statusText;
    }

    public LiveData<FakeBaseStationCell> getThreatAlertEvent() {
        return threatAlertEvent;
    }

    public void clearThreatAlertEvent() {
        threatAlertEvent.setValue(null);
    }

    @Override
    protected void onCleared() {
        detector.stop();
        super.onCleared();
    }
}
