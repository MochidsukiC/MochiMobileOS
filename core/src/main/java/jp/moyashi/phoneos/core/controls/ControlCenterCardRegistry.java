package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.service.SettingsManager;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * コントロールセンターカードの中央レジストリ。
 * カードの登録/削除、配置情報の管理、永続化を担当する。
 */
public class ControlCenterCardRegistry {

    /** 設定キー: カード配置情報 */
    private static final String SETTINGS_KEY_PLACEMENTS = "control_center.card_placements";

    /** 登録されたカード（IDでインデックス） */
    private final Map<String, IControlCenterItem> registeredCards = new LinkedHashMap<>();

    /** 配置情報（IDでインデックス） */
    private final Map<String, CardPlacement> placements = new HashMap<>();

    /** 設定マネージャー */
    private final SettingsManager settingsManager;

    /** リスナー */
    private final List<CardRegistryListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * ControlCenterCardRegistryを作成する。
     *
     * @param settingsManager 設定マネージャー
     */
    public ControlCenterCardRegistry(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        loadPlacements();
        System.out.println("ControlCenterCardRegistry: Initialized with " + placements.size() + " saved placements");
    }

    // === カード登録 ===

    /**
     * カードを登録する。
     * 外部アプリ用のカードは自動的にownerAppIdが設定される。
     *
     * @param card 登録するカード
     * @return 登録成功時true、既に登録済みの場合false
     */
    public synchronized boolean registerCard(IControlCenterItem card) {
        if (card == null || card.getId() == null) {
            return false;
        }

        String cardId = card.getId();

        if (registeredCards.containsKey(cardId)) {
            System.out.println("ControlCenterCardRegistry: Card already registered: " + cardId);
            return false;
        }

        registeredCards.put(cardId, card);

        // 配置情報がなければデフォルトで作成
        if (!placements.containsKey(cardId)) {
            CardPlacement placement = createDefaultPlacement(card);
            placements.put(cardId, placement);
        }

        notifyCardAdded(card);
        System.out.println("ControlCenterCardRegistry: Registered card: " + cardId);
        return true;
    }

    /**
     * カードを登録解除する。
     *
     * @param cardId 削除するカードのID
     * @return 削除成功時true
     */
    public synchronized boolean unregisterCard(String cardId) {
        IControlCenterItem removed = registeredCards.remove(cardId);
        if (removed != null) {
            notifyCardRemoved(removed);
            System.out.println("ControlCenterCardRegistry: Unregistered card: " + cardId);
            return true;
        }
        return false;
    }

    /**
     * 指定アプリのカードを全て削除する（アプリアンインストール時）。
     *
     * @param appId アプリケーションID
     */
    public synchronized void removeCardsByApp(String appId) {
        if (appId == null) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        for (CardPlacement placement : placements.values()) {
            if (appId.equals(placement.getOwnerAppId())) {
                toRemove.add(placement.getCardId());
            }
        }

        for (String cardId : toRemove) {
            unregisterCard(cardId);
            placements.remove(cardId);
        }

        if (!toRemove.isEmpty()) {
            savePlacements();
            System.out.println("ControlCenterCardRegistry: Removed " + toRemove.size() + " cards for app: " + appId);
        }
    }

    // === カード取得 ===

    /**
     * カードをIDで取得する。
     *
     * @param cardId カードID
     * @return カード、見つからない場合はnull
     */
    public IControlCenterItem getCard(String cardId) {
        return registeredCards.get(cardId);
    }

    /**
     * 登録されている全てのカードを取得する。
     *
     * @return カードのリスト（読み取り専用）
     */
    public List<IControlCenterItem> getAllCards() {
        return new ArrayList<>(registeredCards.values());
    }

    /**
     * 指定セクションの表示対象カードを順序どおりに取得する。
     *
     * @param section セクション
     * @return カードのリスト
     */
    public List<IControlCenterItem> getCardsForSection(ControlCenterSection section) {
        return placements.values().stream()
                .filter(p -> p.getSection() == section && p.isVisible())
                .sorted(Comparator.comparingInt(CardPlacement::getOrder))
                .map(p -> registeredCards.get(p.getCardId()))
                .filter(Objects::nonNull)
                .filter(IControlCenterItem::isVisible)
                .collect(Collectors.toList());
    }

    /**
     * 全ての表示対象カードをセクション順・カード順で取得する。
     *
     * @return カードのリスト
     */
    public List<IControlCenterItem> getAllVisibleCards() {
        return Arrays.stream(ControlCenterSection.values())
                .sorted(Comparator.comparingInt(ControlCenterSection::getDefaultOrder))
                .flatMap(section -> getCardsForSection(section).stream())
                .collect(Collectors.toList());
    }

    /**
     * 全ての配置情報を取得する。
     *
     * @return 配置情報のリスト
     */
    public List<CardPlacement> getAllPlacements() {
        return new ArrayList<>(placements.values());
    }

    /**
     * 指定カードの配置情報を取得する。
     *
     * @param cardId カードID
     * @return 配置情報、見つからない場合はnull
     */
    public CardPlacement getPlacement(String cardId) {
        return placements.get(cardId);
    }

    // === 配置変更 ===

    /**
     * カードの配置情報を更新する。
     *
     * @param cardId カードID
     * @param section 新しいセクション
     * @param order 新しい順序
     * @param visible 新しい表示状態
     */
    public void updatePlacement(String cardId, ControlCenterSection section, int order, boolean visible) {
        CardPlacement placement = placements.get(cardId);
        if (placement != null) {
            placement.setSection(section);
            placement.setOrder(order);
            placement.setVisible(visible);
            notifyPlacementChanged(cardId);
        }
    }

    /**
     * カードの表示/非表示を切り替える。
     *
     * @param cardId カードID
     * @param visible 表示状態
     */
    public void setCardVisible(String cardId, boolean visible) {
        CardPlacement placement = placements.get(cardId);
        if (placement != null) {
            placement.setVisible(visible);
            notifyPlacementChanged(cardId);
        }
    }

    /**
     * セクション内でカードの順序を変更する。
     *
     * @param cardId カードID
     * @param newOrder 新しい順序
     */
    public void moveCard(String cardId, int newOrder) {
        CardPlacement placement = placements.get(cardId);
        if (placement != null) {
            placement.setOrder(newOrder);
            notifyPlacementChanged(cardId);
        }
    }

    /**
     * カードを上に移動する（順序を小さくする）。
     *
     * @param cardId カードID
     */
    public void moveCardUp(String cardId) {
        CardPlacement placement = placements.get(cardId);
        if (placement != null) {
            List<CardPlacement> sectionPlacements = getSectionPlacements(placement.getSection());
            int currentIndex = -1;
            for (int i = 0; i < sectionPlacements.size(); i++) {
                if (sectionPlacements.get(i).getCardId().equals(cardId)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex > 0) {
                // 前のカードと順序を入れ替え
                CardPlacement prev = sectionPlacements.get(currentIndex - 1);
                int tempOrder = placement.getOrder();
                placement.setOrder(prev.getOrder());
                prev.setOrder(tempOrder);
                notifyPlacementChanged(cardId);
                notifyPlacementChanged(prev.getCardId());
            }
        }
    }

    /**
     * カードを下に移動する（順序を大きくする）。
     *
     * @param cardId カードID
     */
    public void moveCardDown(String cardId) {
        CardPlacement placement = placements.get(cardId);
        if (placement != null) {
            List<CardPlacement> sectionPlacements = getSectionPlacements(placement.getSection());
            int currentIndex = -1;
            for (int i = 0; i < sectionPlacements.size(); i++) {
                if (sectionPlacements.get(i).getCardId().equals(cardId)) {
                    currentIndex = i;
                    break;
                }
            }
            if (currentIndex >= 0 && currentIndex < sectionPlacements.size() - 1) {
                // 次のカードと順序を入れ替え
                CardPlacement next = sectionPlacements.get(currentIndex + 1);
                int tempOrder = placement.getOrder();
                placement.setOrder(next.getOrder());
                next.setOrder(tempOrder);
                notifyPlacementChanged(cardId);
                notifyPlacementChanged(next.getCardId());
            }
        }
    }

    /**
     * 指定セクションの配置情報を順序付きで取得する。
     */
    private List<CardPlacement> getSectionPlacements(ControlCenterSection section) {
        return placements.values().stream()
                .filter(p -> p.getSection() == section)
                .sorted(Comparator.comparingInt(CardPlacement::getOrder))
                .collect(Collectors.toList());
    }

    // === 永続化 ===

    /**
     * 配置情報を永続化する。
     */
    public void savePlacements() {
        if (settingsManager == null) {
            return;
        }

        List<Map<String, Object>> list = placements.values().stream()
                .map(CardPlacement::toMap)
                .collect(Collectors.toList());

        settingsManager.setSetting(SETTINGS_KEY_PLACEMENTS, list);
        settingsManager.saveSettings();
        System.out.println("ControlCenterCardRegistry: Saved " + list.size() + " placements");
    }

    /**
     * 配置情報を読み込む。
     */
    @SuppressWarnings("unchecked")
    private void loadPlacements() {
        if (settingsManager == null) {
            return;
        }

        Object raw = settingsManager.getSetting(SETTINGS_KEY_PLACEMENTS);
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    try {
                        CardPlacement placement = CardPlacement.fromMap((Map<String, Object>) map);
                        if (placement != null && placement.getCardId() != null) {
                            placements.put(placement.getCardId(), placement);
                        }
                    } catch (Exception e) {
                        System.err.println("ControlCenterCardRegistry: Error loading placement: " + e.getMessage());
                    }
                }
            }
        }
    }

    // === ヘルパー ===

    /**
     * カードのデフォルト配置情報を作成する。
     */
    private CardPlacement createDefaultPlacement(IControlCenterItem card) {
        CardPlacement placement = new CardPlacement();
        placement.setCardId(card.getId());
        placement.setVisible(true);

        if (card instanceof IAppControlCenterItem appCard) {
            placement.setOwnerAppId(appCard.getOwnerApplicationId());
            placement.setSection(appCard.getSection());
            placement.setOrder(appCard.getDefaultOrder());
        } else {
            // システムカードはIDからセクションを推測
            placement.setSection(inferSection(card));
            placement.setOrder(inferOrder(card));
        }

        return placement;
    }

    /**
     * カードIDからセクションを推測する。
     */
    private ControlCenterSection inferSection(IControlCenterItem card) {
        String id = card.getId().toLowerCase();
        if (id.contains("wifi") || id.contains("bluetooth") || id.contains("airplane") ||
                id.contains("silent") || id.contains("flashlight")) {
            return ControlCenterSection.QUICK_SETTINGS;
        }
        if (id.contains("volume") || id.contains("now_playing") || id.contains("media")) {
            return ControlCenterSection.MEDIA;
        }
        if (id.contains("brightness") || id.contains("dark_mode") || id.contains("rotate") ||
                id.contains("night") || id.contains("display")) {
            return ControlCenterSection.DISPLAY;
        }
        return ControlCenterSection.APP_WIDGETS;
    }

    /**
     * カードIDからデフォルト順序を推測する。
     */
    private int inferOrder(IControlCenterItem card) {
        String id = card.getId().toLowerCase();
        // 重要なカードを先頭に
        if (id.contains("wifi")) return 0;
        if (id.contains("bluetooth")) return 1;
        if (id.contains("silent")) return 2;
        if (id.contains("airplane")) return 3;
        if (id.contains("brightness")) return 0;
        if (id.contains("volume")) return 0;
        if (id.contains("now_playing")) return 1;
        return 50;
    }

    // === リスナー ===

    /**
     * カード登録/削除/配置変更のリスナーインターフェース。
     */
    public interface CardRegistryListener {
        /** カードが追加された時 */
        void onCardAdded(IControlCenterItem card);

        /** カードが削除された時 */
        void onCardRemoved(IControlCenterItem card);

        /** カードの配置が変更された時 */
        void onPlacementChanged(String cardId);
    }

    public void addListener(CardRegistryListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(CardRegistryListener listener) {
        listeners.remove(listener);
    }

    private void notifyCardAdded(IControlCenterItem card) {
        for (CardRegistryListener l : listeners) {
            try {
                l.onCardAdded(card);
            } catch (Exception e) {
                System.err.println("ControlCenterCardRegistry: Error notifying listener: " + e.getMessage());
            }
        }
    }

    private void notifyCardRemoved(IControlCenterItem card) {
        for (CardRegistryListener l : listeners) {
            try {
                l.onCardRemoved(card);
            } catch (Exception e) {
                System.err.println("ControlCenterCardRegistry: Error notifying listener: " + e.getMessage());
            }
        }
    }

    private void notifyPlacementChanged(String cardId) {
        for (CardRegistryListener l : listeners) {
            try {
                l.onPlacementChanged(cardId);
            } catch (Exception e) {
                System.err.println("ControlCenterCardRegistry: Error notifying listener: " + e.getMessage());
            }
        }
    }
}
