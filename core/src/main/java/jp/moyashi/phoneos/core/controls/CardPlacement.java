package jp.moyashi.phoneos.core.controls;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * カードの配置情報。設定で永続化される。
 * JSONシリアライズ可能な構造を持つ。
 */
public class CardPlacement {

    /** カードID */
    private String cardId;

    /** 所属セクション */
    private ControlCenterSection section;

    /** セクション内の表示順序（小さい値が先頭） */
    private int order;

    /** 表示状態 */
    private boolean visible;

    /** 提供元アプリID（システムカードはnull） */
    private String ownerAppId;

    /**
     * デフォルトコンストラクタ。
     */
    public CardPlacement() {
        this.section = ControlCenterSection.APP_WIDGETS;
        this.order = 50;
        this.visible = true;
    }

    /**
     * フルコンストラクタ。
     *
     * @param cardId カードID
     * @param section セクション
     * @param order 表示順序
     * @param visible 表示状態
     * @param ownerAppId 提供元アプリID
     */
    public CardPlacement(String cardId, ControlCenterSection section, int order, boolean visible, String ownerAppId) {
        this.cardId = cardId;
        this.section = section;
        this.order = order;
        this.visible = visible;
        this.ownerAppId = ownerAppId;
    }

    // === Getters ===

    public String getCardId() {
        return cardId;
    }

    public ControlCenterSection getSection() {
        return section;
    }

    public int getOrder() {
        return order;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getOwnerAppId() {
        return ownerAppId;
    }

    // === Setters ===

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public void setSection(ControlCenterSection section) {
        this.section = section;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setOwnerAppId(String ownerAppId) {
        this.ownerAppId = ownerAppId;
    }

    // === Serialization ===

    /**
     * JSON永続化用のMapに変換する。
     *
     * @return Map形式のデータ
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("cardId", cardId);
        map.put("section", section != null ? section.name() : ControlCenterSection.APP_WIDGETS.name());
        map.put("order", order);
        map.put("visible", visible);
        if (ownerAppId != null) {
            map.put("ownerAppId", ownerAppId);
        }
        return map;
    }

    /**
     * Mapから復元する。
     *
     * @param map Map形式のデータ
     * @return CardPlacementインスタンス、復元失敗時はnull
     */
    public static CardPlacement fromMap(Map<String, Object> map) {
        if (map == null || !map.containsKey("cardId")) {
            return null;
        }

        CardPlacement placement = new CardPlacement();

        Object cardIdObj = map.get("cardId");
        if (cardIdObj instanceof String) {
            placement.setCardId((String) cardIdObj);
        } else {
            return null; // cardIdは必須
        }

        Object sectionObj = map.get("section");
        if (sectionObj instanceof String) {
            placement.setSection(ControlCenterSection.fromName((String) sectionObj));
        }

        Object orderObj = map.get("order");
        if (orderObj instanceof Number) {
            placement.setOrder(((Number) orderObj).intValue());
        }

        Object visibleObj = map.get("visible");
        if (visibleObj instanceof Boolean) {
            placement.setVisible((Boolean) visibleObj);
        }

        Object ownerAppIdObj = map.get("ownerAppId");
        if (ownerAppIdObj instanceof String) {
            placement.setOwnerAppId((String) ownerAppIdObj);
        }

        return placement;
    }

    // === Object methods ===

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardPlacement that = (CardPlacement) o;
        return Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId);
    }

    @Override
    public String toString() {
        return "CardPlacement{" +
                "cardId='" + cardId + '\'' +
                ", section=" + section +
                ", order=" + order +
                ", visible=" + visible +
                ", ownerAppId='" + ownerAppId + '\'' +
                '}';
    }
}
