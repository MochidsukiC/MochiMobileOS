package jp.moyashi.phoneos.core.dashboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ダッシュボードスロットへのウィジェット割り当て情報。
 * JSON永続化用のシリアライズ/デシリアライズをサポート。
 */
public class DashboardSlotAssignment {

    /** スロット */
    private DashboardSlot slot;

    /** 割り当てられたウィジェットID */
    private String widgetId;

    /**
     * デフォルトコンストラクタ。
     */
    public DashboardSlotAssignment() {
    }

    /**
     * コンストラクタ。
     *
     * @param slot スロット
     * @param widgetId ウィジェットID
     */
    public DashboardSlotAssignment(DashboardSlot slot, String widgetId) {
        this.slot = slot;
        this.widgetId = widgetId;
    }

    // === Getters ===

    public DashboardSlot getSlot() {
        return slot;
    }

    public String getWidgetId() {
        return widgetId;
    }

    // === Setters ===

    public void setSlot(DashboardSlot slot) {
        this.slot = slot;
    }

    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    // === Serialization ===

    /**
     * JSON永続化用のMapに変換する。
     *
     * @return Map形式のデータ
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("slot", slot != null ? slot.name() : null);
        map.put("widgetId", widgetId);
        return map;
    }

    /**
     * Mapから復元する。
     *
     * @param map Map形式のデータ
     * @return DashboardSlotAssignmentインスタンス、復元失敗時はnull
     */
    public static DashboardSlotAssignment fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        DashboardSlotAssignment assignment = new DashboardSlotAssignment();

        Object slotObj = map.get("slot");
        if (slotObj instanceof String) {
            assignment.setSlot(DashboardSlot.fromName((String) slotObj));
        } else if (slotObj instanceof Number) {
            assignment.setSlot(DashboardSlot.fromIndex(((Number) slotObj).intValue()));
        }

        Object widgetIdObj = map.get("widgetId");
        if (widgetIdObj instanceof String) {
            assignment.setWidgetId((String) widgetIdObj);
        }

        return assignment;
    }

    // === Object methods ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardSlotAssignment that = (DashboardSlotAssignment) o;
        return slot == that.slot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot);
    }

    @Override
    public String toString() {
        return "DashboardSlotAssignment{" +
                "slot=" + slot +
                ", widgetId='" + widgetId + '\'' +
                '}';
    }
}
