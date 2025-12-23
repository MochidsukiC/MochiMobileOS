package jp.moyashi.phoneos.core.dashboard;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.SettingsManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ダッシュボードウィジェットの中央レジストリ。
 * ウィジェットの登録/削除、スロット割り当て、永続化を担当する。
 */
public class DashboardWidgetRegistry {

    /** 設定キー: スロット割り当て */
    private static final String SETTINGS_KEY_ASSIGNMENTS = "dashboard.slot_assignments";

    /** 登録されたウィジェット（IDでインデックス） */
    private final Map<String, IDashboardWidget> registeredWidgets = new LinkedHashMap<>();

    /** スロット割り当て（スロットでインデックス） */
    private final Map<DashboardSlot, String> slotAssignments = new EnumMap<>(DashboardSlot.class);

    /** 設定マネージャー */
    private final SettingsManager settingsManager;

    /** カーネル参照 */
    private Kernel kernel;

    /** リスナー */
    private final List<WidgetRegistryListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * DashboardWidgetRegistryを作成する。
     *
     * @param settingsManager 設定マネージャー
     */
    public DashboardWidgetRegistry(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        loadAssignments();
        System.out.println("DashboardWidgetRegistry: Initialized with " + slotAssignments.size() + " saved assignments");
    }

    /**
     * カーネル参照を設定する。
     *
     * @param kernel カーネル
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    // === ウィジェット登録 ===

    /**
     * ウィジェットを登録する。
     *
     * @param widget 登録するウィジェット
     * @return 登録成功時true、既に登録済みの場合false
     */
    public synchronized boolean registerWidget(IDashboardWidget widget) {
        if (widget == null || widget.getId() == null) {
            return false;
        }

        String widgetId = widget.getId();

        if (registeredWidgets.containsKey(widgetId)) {
            System.out.println("DashboardWidgetRegistry: Widget already registered: " + widgetId);
            return false;
        }

        registeredWidgets.put(widgetId, widget);
        notifyWidgetAdded(widget);
        System.out.println("DashboardWidgetRegistry: Registered widget: " + widgetId +
                " (size=" + widget.getSize() + ", type=" + widget.getType() + ")");
        return true;
    }

    /**
     * ウィジェットを登録解除する。
     *
     * @param widgetId 削除するウィジェットのID
     * @return 削除成功時true
     */
    public synchronized boolean unregisterWidget(String widgetId) {
        IDashboardWidget removed = registeredWidgets.remove(widgetId);
        if (removed != null) {
            // このウィジェットが割り当てられているスロットをクリア
            slotAssignments.entrySet().removeIf(entry -> widgetId.equals(entry.getValue()));
            removed.onDetach();
            notifyWidgetRemoved(removed);
            System.out.println("DashboardWidgetRegistry: Unregistered widget: " + widgetId);
            return true;
        }
        return false;
    }

    /**
     * 指定アプリのウィジェットを全て削除する（アプリアンインストール時）。
     *
     * @param appId アプリケーションID
     */
    public synchronized void removeWidgetsByApp(String appId) {
        if (appId == null) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        for (IDashboardWidget widget : registeredWidgets.values()) {
            if (appId.equals(widget.getOwnerApplicationId())) {
                toRemove.add(widget.getId());
            }
        }

        for (String widgetId : toRemove) {
            unregisterWidget(widgetId);
        }

        if (!toRemove.isEmpty()) {
            saveAssignments();
            System.out.println("DashboardWidgetRegistry: Removed " + toRemove.size() + " widgets for app: " + appId);
        }
    }

    // === ウィジェット取得 ===

    /**
     * ウィジェットをIDで取得する。
     *
     * @param widgetId ウィジェットID
     * @return ウィジェット、見つからない場合はnull
     */
    public IDashboardWidget getWidget(String widgetId) {
        return registeredWidgets.get(widgetId);
    }

    /**
     * 登録されている全てのウィジェットを取得する。
     *
     * @return ウィジェットのリスト
     */
    public List<IDashboardWidget> getAllWidgets() {
        return new ArrayList<>(registeredWidgets.values());
    }

    /**
     * 指定サイズのウィジェット一覧を取得する。
     *
     * @param size サイズ
     * @return ウィジェットのリスト
     */
    public List<IDashboardWidget> getWidgetsBySize(DashboardWidgetSize size) {
        return registeredWidgets.values().stream()
                .filter(w -> w.getSize() == size)
                .collect(Collectors.toList());
    }

    /**
     * スロットに割り当て可能なウィジェット一覧を取得する。
     *
     * @param slot スロット
     * @return ウィジェットのリスト
     */
    public List<IDashboardWidget> getAvailableWidgetsForSlot(DashboardSlot slot) {
        DashboardWidgetSize requiredSize = slot.getRequiredSize();
        return getWidgetsBySize(requiredSize);
    }

    // === スロット割り当て ===

    /**
     * スロットに割り当てられたウィジェットを取得する。
     *
     * @param slot スロット
     * @return ウィジェット、割り当てがない場合はnull
     */
    public IDashboardWidget getWidgetForSlot(DashboardSlot slot) {
        String widgetId = slotAssignments.get(slot);
        if (widgetId == null) {
            return null;
        }
        return registeredWidgets.get(widgetId);
    }

    /**
     * スロットにウィジェットを割り当てる。
     *
     * @param slot スロット
     * @param widgetId ウィジェットID（nullで割り当て解除）
     * @return 割り当て成功時true
     */
    public synchronized boolean assignWidgetToSlot(DashboardSlot slot, String widgetId) {
        if (slot == null || !slot.isConfigurable()) {
            System.out.println("DashboardWidgetRegistry: Slot is not configurable: " + slot);
            return false;
        }

        // 既存の割り当てを解除
        String oldWidgetId = slotAssignments.get(slot);
        if (oldWidgetId != null) {
            IDashboardWidget oldWidget = registeredWidgets.get(oldWidgetId);
            if (oldWidget != null) {
                oldWidget.onDetach();
            }
        }

        if (widgetId == null) {
            // 割り当て解除
            slotAssignments.remove(slot);
            notifyAssignmentChanged(slot, null);
            return true;
        }

        // ウィジェットの存在確認
        IDashboardWidget widget = registeredWidgets.get(widgetId);
        if (widget == null) {
            System.out.println("DashboardWidgetRegistry: Widget not found: " + widgetId);
            return false;
        }

        // サイズ互換性チェック
        if (widget.getSize() != slot.getRequiredSize()) {
            System.out.println("DashboardWidgetRegistry: Widget size mismatch: " +
                    widget.getSize() + " != " + slot.getRequiredSize());
            return false;
        }

        // 割り当て
        slotAssignments.put(slot, widgetId);
        if (kernel != null) {
            widget.onAttach(kernel);
        }
        notifyAssignmentChanged(slot, widgetId);
        System.out.println("DashboardWidgetRegistry: Assigned " + widgetId + " to " + slot);
        return true;
    }

    /**
     * 全スロットの割り当て情報を取得する。
     *
     * @return スロットとウィジェットIDのマップ
     */
    public Map<DashboardSlot, String> getAllAssignments() {
        return new EnumMap<>(slotAssignments);
    }

    // === 永続化 ===

    /**
     * スロット割り当てを永続化する。
     */
    public void saveAssignments() {
        if (settingsManager == null) {
            return;
        }

        Map<String, String> map = new HashMap<>();
        for (Map.Entry<DashboardSlot, String> entry : slotAssignments.entrySet()) {
            map.put(String.valueOf(entry.getKey().getIndex()), entry.getValue());
        }

        settingsManager.setSetting(SETTINGS_KEY_ASSIGNMENTS, map);
        settingsManager.saveSettings();
        System.out.println("DashboardWidgetRegistry: Saved " + map.size() + " assignments");
    }

    /**
     * スロット割り当てを読み込む。
     */
    @SuppressWarnings("unchecked")
    private void loadAssignments() {
        if (settingsManager == null) {
            return;
        }

        Object raw = settingsManager.getSetting(SETTINGS_KEY_ASSIGNMENTS);
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                try {
                    int slotIndex;
                    if (entry.getKey() instanceof String) {
                        slotIndex = Integer.parseInt((String) entry.getKey());
                    } else if (entry.getKey() instanceof Number) {
                        slotIndex = ((Number) entry.getKey()).intValue();
                    } else {
                        continue;
                    }

                    DashboardSlot slot = DashboardSlot.fromIndex(slotIndex);
                    if (slot != null && slot.isConfigurable() && entry.getValue() instanceof String) {
                        slotAssignments.put(slot, (String) entry.getValue());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("DashboardWidgetRegistry: Error parsing slot index: " + entry.getKey());
                }
            }
        }
    }

    /**
     * デフォルトの割り当てを設定する（初回起動時）。
     */
    public void setDefaultAssignments() {
        // デフォルトのシステムウィジェットを割り当て
        if (!slotAssignments.containsKey(DashboardSlot.SEARCH)) {
            slotAssignments.put(DashboardSlot.SEARCH, "system.search");
        }
        if (!slotAssignments.containsKey(DashboardSlot.LEFT)) {
            slotAssignments.put(DashboardSlot.LEFT, "system.messages");
        }
        if (!slotAssignments.containsKey(DashboardSlot.RIGHT)) {
            slotAssignments.put(DashboardSlot.RIGHT, "system.emoney");
        }
        if (!slotAssignments.containsKey(DashboardSlot.BOTTOM)) {
            slotAssignments.put(DashboardSlot.BOTTOM, "system.ai_assistant");
        }

        // ウィジェットにonAttachを呼び出す
        if (kernel != null) {
            for (Map.Entry<DashboardSlot, String> entry : slotAssignments.entrySet()) {
                IDashboardWidget widget = registeredWidgets.get(entry.getValue());
                if (widget != null) {
                    widget.onAttach(kernel);
                }
            }
        }
    }

    // === リスナー ===

    /**
     * ウィジェット登録/削除/割り当て変更のリスナーインターフェース。
     */
    public interface WidgetRegistryListener {
        /** ウィジェットが追加された時 */
        default void onWidgetAdded(IDashboardWidget widget) {}

        /** ウィジェットが削除された時 */
        default void onWidgetRemoved(IDashboardWidget widget) {}

        /** スロット割り当てが変更された時 */
        default void onAssignmentChanged(DashboardSlot slot, String widgetId) {}
    }

    public void addListener(WidgetRegistryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(WidgetRegistryListener listener) {
        listeners.remove(listener);
    }

    private void notifyWidgetAdded(IDashboardWidget widget) {
        for (WidgetRegistryListener l : listeners) {
            try {
                l.onWidgetAdded(widget);
            } catch (Exception e) {
                System.err.println("DashboardWidgetRegistry: Error notifying listener: " + e.getMessage());
            }
        }
    }

    private void notifyWidgetRemoved(IDashboardWidget widget) {
        for (WidgetRegistryListener l : listeners) {
            try {
                l.onWidgetRemoved(widget);
            } catch (Exception e) {
                System.err.println("DashboardWidgetRegistry: Error notifying listener: " + e.getMessage());
            }
        }
    }

    private void notifyAssignmentChanged(DashboardSlot slot, String widgetId) {
        for (WidgetRegistryListener l : listeners) {
            try {
                l.onAssignmentChanged(slot, widgetId);
            } catch (Exception e) {
                System.err.println("DashboardWidgetRegistry: Error notifying listener: " + e.getMessage());
            }
        }
    }
}
