package org.zacsn.signal_dectect.data.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.PresenceState;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯 JVM 单测：验证聚合引擎的合并语义、扫描类型过滤、在线状态跃迁、
 * 轮次隔离与回调节流。
 *
 * 不引入 Robolectric / Mockito —— 引擎的时钟与调度都是构造参数，
 * 这里用同步假实现即可获得完全确定的断言。
 */
public class ScanEngineTest {

    private FakeClock clock;
    private FakeScheduler scheduler;
    private RecordingListener listener;
    private ScanEngine engine;

    @Before
    public void setUp() {
        clock = new FakeClock();
        scheduler = new FakeScheduler();
        listener = new RecordingListener();
        engine = new ScanEngine(clock, scheduler);
        engine.setListener(listener);
    }

    // ---- 合并语义 ----

    @Test
    public void secondObservationMergesInsteadOfOverwriting() {
        engine.start(ScanType.ALL);

        // 第一条：GATT 已确认厂商，置信度高。
        engine.submit(device("AA:BB:CC:DD:EE:FF", "耳机", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "gatt_device_info", 100, ManufacturerVerdict.CONFIRMED,
                -60, 1000L));
        // 第二条：只有 OUI 猜测，置信度低，且名称是占位文案。
        engine.submit(device("AA:BB:CC:DD:EE:FF", "unknown", DeviceType.BLUETOOTH_LE,
                "未确认", "oui", 30, ManufacturerVerdict.POSSIBLE,
                -70, 2000L));

        List<SignalDevice> devices = engine.allDevices();
        assertEquals(1, devices.size());
        SignalDevice merged = devices.get(0);

        // 身份信息整体来自置信度更高的一方，不被后来的低置信度观测顶掉。
        assertEquals("Apple, Inc.", merged.getManufacturer());
        assertEquals(100, merged.getManufacturerConfidence());
        assertEquals(ManufacturerVerdict.CONFIRMED, merged.getManufacturerVerdict());
        // 占位名称不覆盖有效名称。
        assertEquals("耳机", merged.getDeviceName());
        // firstSeen 取历史值，lastSeen 与信号强度取最新值。
        assertEquals(1000L, merged.getFirstSeen());
        assertEquals(2000L, merged.getLastSeen());
        assertEquals(-70, merged.getSignalStrength());
    }

    @Test
    public void userFlagsSurviveNewObservations() {
        engine.start(ScanType.ALL);

        SignalDevice flagged = new SignalDevice(
                "11:22:33:44:55:66", "目标", DeviceType.BLUETOOTH_LE,
                "Sony", "Sony", "oui", 60,
                ManufacturerVerdict.POSSIBLE, "",
                -55, null, 1.0, 1000L, 1000L,
                true, true, false,
                PresenceState.ACTIVE, 0L);
        engine.submit(flagged);
        engine.submit(device("11:22:33:44:55:66", "目标", DeviceType.BLUETOOTH_LE,
                "Sony", "oui", 60, ManufacturerVerdict.POSSIBLE, -58, 2000L));

        SignalDevice merged = engine.allDevices().get(0);
        assertTrue(merged.isFocused());
        assertTrue(merged.isBlacklisted());
    }

    // ---- 扫描类型过滤 ----

    @Test
    public void wifiOnlyScanDropsBluetoothObservations() {
        engine.start(ScanType.WIFI_ONLY);

        engine.submit(device("AA:BB:CC:DD:EE:FF", "蓝牙设备", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));
        engine.submit(device("00:11:22:33:44:55", "热点", DeviceType.WIFI,
                "TP-Link", "oui", 60, ManufacturerVerdict.POSSIBLE, -50, 1000L));

        List<SignalDevice> devices = engine.allDevices();
        assertEquals(1, devices.size());
        assertEquals(DeviceType.WIFI, devices.get(0).getDeviceType());
    }

    // ---- 在线状态跃迁 ----

    @Test
    public void presenceStateWalksFromActiveToMissingToStaleThenRemoved() {
        engine.start(ScanType.ALL);
        engine.submit(device("AA:BB:CC:DD:EE:FF", "耳机", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));

        assertEquals(PresenceState.ACTIVE, single().getPresenceState());

        // 刚过 MISSING 阈值。
        advanceAndTick(ScanTuning.BLE_MISSING_TIMEOUT_MS + 1);
        assertEquals(PresenceState.MISSING, single().getPresenceState());

        // 再过 STALE 阈值。
        advanceAndTick(ScanTuning.BLE_STALE_TIMEOUT_MS - ScanTuning.BLE_MISSING_TIMEOUT_MS);
        assertEquals(PresenceState.STALE, single().getPresenceState());

        // 保留期满后从列表移除。
        advanceAndTick(ScanTuning.STALE_RETENTION_MS + 1);
        assertTrue(engine.allDevices().isEmpty());
    }

    @Test
    public void freshObservationRestoresActiveState() {
        engine.start(ScanType.ALL);
        engine.submit(device("AA:BB:CC:DD:EE:FF", "耳机", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));

        advanceAndTick(ScanTuning.BLE_MISSING_TIMEOUT_MS + 1);
        assertEquals(PresenceState.MISSING, single().getPresenceState());

        engine.submit(device("AA:BB:CC:DD:EE:FF", "耳机", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -62, 2000L));
        assertEquals(PresenceState.ACTIVE, single().getPresenceState());
    }

    @Test
    public void wifiUsesItsOwnTimeoutInsteadOfTheBleOne() {
        engine.start(ScanType.ALL);
        engine.submit(device("00:11:22:33:44:55", "热点", DeviceType.WIFI,
                "TP-Link", "oui", 60, ManufacturerVerdict.POSSIBLE, -50, 1000L));

        // 已超过 BLE 的 MISSING 阈值，但仍在 WiFi 阈值之内。
        advanceAndTick(ScanTuning.BLE_MISSING_TIMEOUT_MS + 1);
        assertEquals(PresenceState.ACTIVE, single().getPresenceState());

        advanceAndTick(ScanTuning.WIFI_MISSING_TIMEOUT_MS);
        assertEquals(PresenceState.MISSING, single().getPresenceState());
    }

    // ---- 轮次隔离 ----

    @Test
    public void newRoundClearsPreviousDevices() {
        engine.start(ScanType.ALL);
        engine.submit(device("AA:BB:CC:DD:EE:FF", "上一轮", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));
        assertEquals(1, engine.allDevices().size());

        engine.start(ScanType.ALL);
        assertTrue(engine.allDevices().isEmpty());
    }

    @Test
    public void observationSubmittedAfterStopIsDropped() {
        engine.start(ScanType.ALL);
        engine.stop();

        engine.submit(device("AA:BB:CC:DD:EE:FF", "迟到", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));

        assertTrue(engine.allDevices().isEmpty());
    }

    /**
     * 采集器在旧一轮里排队的观测，不能落进新一轮的表。
     *
     * 用延迟执行的调度器复现：submit 时捕获的是旧轮次号，真正执行时已经 start 过一次。
     */
    @Test
    public void queuedObservationFromPreviousRoundIsDropped() {
        engine.start(ScanType.ALL);
        scheduler.drain();

        scheduler.setDeferred(true);
        engine.submit(device("AA:BB:CC:DD:EE:FF", "旧轮次", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));
        scheduler.setDeferred(false);

        engine.start(ScanType.ALL);   // 新一轮
        scheduler.drain();            // 此时旧观测才被执行

        assertTrue(engine.allDevices().isEmpty());
    }

    // ---- 回调节流 ----

    @Test
    public void manySubmitsProduceASingleListenerCallback() {
        engine.start(ScanType.ALL);
        listener.reset();

        for (int i = 0; i < 20; i++) {
            engine.submit(device("AA:BB:CC:DD:EE:0" + (i % 3), "设备" + i, DeviceType.BLUETOOTH_LE,
                    "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L + i));
        }

        // 变更只记脏，尚未回调；但快照必须是新鲜的（stopScan 会先读快照再停）。
        assertEquals(0, listener.callCount);
        assertEquals(3, engine.allDevices().size());

        scheduler.tick();
        assertEquals(1, listener.callCount);
        assertEquals(3, listener.lastDevices.size());

        // 没有新变更时不重复回调。
        scheduler.tick();
        assertEquals(1, listener.callCount);
    }

    @Test
    public void stopFlushesFinalSnapshot() {
        engine.start(ScanType.ALL);
        listener.reset();

        engine.submit(device("AA:BB:CC:DD:EE:FF", "耳机", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));
        assertEquals(0, listener.callCount);

        engine.stop();
        assertEquals(1, listener.callCount);
        assertEquals(1, listener.lastDevices.size());
        // 停止后设备列表保留，供保存扫描记录使用。
        assertEquals(1, engine.sessionDevices().size());
    }

    @Test
    public void sessionDevicesKeepRemovedDevices() {
        engine.start(ScanType.ALL);
        engine.submit(device("AA:BB:CC:DD:EE:FF", "耳机", DeviceType.BLUETOOTH_LE,
                "Apple, Inc.", "oui", 60, ManufacturerVerdict.POSSIBLE, -60, 1000L));

        advanceAndTick(ScanTuning.BLE_STALE_TIMEOUT_MS + ScanTuning.STALE_RETENTION_MS + 1);

        assertTrue(engine.allDevices().isEmpty());
        assertEquals(1, engine.sessionDevices().size());
    }

    // ---- 辅助 ----

    private SignalDevice single() {
        List<SignalDevice> devices = engine.allDevices();
        assertEquals(1, devices.size());
        return devices.get(0);
    }

    private void advanceAndTick(long deltaMs) {
        clock.advance(deltaMs);
        scheduler.tick();
    }

    /**
     * 构造观测。必须使用带 presenceState / lastSeenElapsedRealtime 的最长构造函数 ——
     * 其余构造函数内部会调 SystemClock.elapsedRealtime()，在纯 JVM 测试里会抛 "not mocked"。
     * 引擎会用注入的时钟重新打上 lastSeenElapsedRealtime，所以这里传 0 即可。
     */
    private static SignalDevice device(String mac, String name, DeviceType type,
                                       String manufacturer, String source, int confidence,
                                       ManufacturerVerdict verdict, int rssi, long timestamp) {
        return new SignalDevice(
                mac, name, type, manufacturer, manufacturer, source, confidence,
                verdict, "", rssi, null, 1.0, timestamp, timestamp,
                false, false, false,
                PresenceState.ACTIVE, 0L);
    }

    private static final class FakeClock implements ScanEngine.Clock {
        private long now = 10_000L;

        @Override
        public long now() {
            return now;
        }

        void advance(long deltaMs) {
            now += deltaMs;
        }
    }

    /**
     * 同步调度器：默认在调用线程立即执行，节拍由测试手动驱动。
     *
     * {@code deferred} 用于模拟「任务已入队但尚未执行」，以复现跨轮次的迟到观测。
     */
    private static final class FakeScheduler implements ScanEngine.Scheduler {
        private final List<Runnable> pending = new ArrayList<>();
        private Runnable periodic;
        private boolean deferred = false;

        @Override
        public void execute(Runnable task) {
            if (deferred) {
                pending.add(task);
            } else {
                task.run();
            }
        }

        @Override
        public ScanEngine.Cancellation schedulePeriodic(long intervalMs, Runnable task) {
            assertEquals(ScanTuning.EMIT_INTERVAL_MS, intervalMs);
            periodic = task;
            return () -> periodic = null;
        }

        @Override
        public void shutdown() {
            periodic = null;
            pending.clear();
        }

        void setDeferred(boolean value) {
            deferred = value;
        }

        void drain() {
            List<Runnable> tasks = new ArrayList<>(pending);
            pending.clear();
            for (Runnable task : tasks) {
                task.run();
            }
        }

        /** 驱动一次节拍（生产环境下每 EMIT_INTERVAL_MS 触发一次）。 */
        void tick() {
            assertNotNull("periodic task should be scheduled while scanning", periodic);
            periodic.run();
        }
    }

    private static final class RecordingListener implements ScanEngine.Listener {
        int callCount = 0;
        List<SignalDevice> lastDevices = new ArrayList<>();

        @Override
        public void onDevicesUpdated(List<? extends SignalDevice> devices) {
            callCount++;
            lastDevices = new ArrayList<>(devices);
        }

        void reset() {
            callCount = 0;
            lastDevices = new ArrayList<>();
        }
    }
}
