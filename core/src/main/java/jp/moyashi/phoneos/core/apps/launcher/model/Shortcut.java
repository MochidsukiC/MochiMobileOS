package jp.moyashi.phoneos.core.apps.launcher.model;

import jp.moyashi.phoneos.core.app.IApplication;

/**
 * ホーム画面上のアプリケーションへのショートカットを表す。
 * ショートカットはそれが表すアプリケーションへの参照、
 * 位置情報およびカスタマイゼーション情報を含む。
 * 
 * ショートカットはホーム画面ページの特定のグリッド位置に配置でき、
 * 元のアプリケーションとは異なるカスタム表示プロパティを持つことができる。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class Shortcut {
    
    /** このショートカットが参照するアプリケーション */
    private final IApplication application;
    
    /** グリッド列位置（0ベース） */
    private int gridX;
    
    /** グリッド行位置（0ベース） */
    private int gridY;
    
    /** ショートカットのカスタム表示名（アプリ名を使用する場合null） */
    private String customName;
    
    /** このショートカットが現在ドラッグされているかどうか */
    private boolean isDragging;
    
    /** ドラッグ中の一時位置（画面座標） */
    private float dragX;
    private float dragY;
    
    /** 編集モードの搖れエフェクト用アニメーションオフセット */
    private float wiggleOffset;
    
    /** このショートカットの一意識別子 */
    private final String shortcutId;

    /** 常時表示フィールド（Dock）内の位置（-1の場合はDockにない） */
    private int dockPosition;

    /** 一意のショートカットID生成用静的カウンター */
    private static int nextId = 1;
    
    /**
     * 指定されたアプリケーション用の新しいショートカットを作成する。
     * 
     * @param application このショートカットが参照するアプリケーション
     * @param gridX 初期グリッド列位置
     * @param gridY 初期グリッド行位置
     */
    public Shortcut(IApplication application, int gridX, int gridY) {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }
        
        this.application = application;
        this.gridX = gridX;
        this.gridY = gridY;
        this.customName = null;
        this.isDragging = false;
        this.dragX = 0;
        this.dragY = 0;
        this.wiggleOffset = 0;
        this.dockPosition = -1; // 初期値はDockにない状態
        this.shortcutId = "shortcut_" + (nextId++);
        
        System.out.println("Shortcut: Created shortcut for " + application.getName() + 
                          " at position (" + gridX + ", " + gridY + ")");
    }
    
    /**
     * 位置(0,0)で指定されたアプリケーション用の新しいショートカットを作成する。
     * 
     * @param application このショートカットが参照するアプリケーション
     */
    public Shortcut(IApplication application) {
        this(application, 0, 0);
    }
    
    /**
     * このショートカットが参照するアプリケーションを取得する。
     * 
     * @return IApplicationインスタンス
     */
    public IApplication getApplication() {
        return application;
    }
    
    /**
     * このショートカットのグリッド列位置を取得する。
     * 
     * @return グリッドX座標（0ベース）
     */
    public int getGridX() {
        return gridX;
    }
    
    /**
     * このショートカットのグリッド列位置を設定する。
     * 
     * @param gridX 新しいグリッドX座標（0ベース）
     */
    public void setGridX(int gridX) {
        this.gridX = gridX;
    }
    
    /**
     * このショートカットのグリッド行位置を取得する。
     * 
     * @return グリッドY座標（0ベース）
     */
    public int getGridY() {
        return gridY;
    }
    
    /**
     * このショートカットのグリッド行位置を設定する。
     * 
     * @param gridY 新しいグリッドY座標（0ベース）
     */
    public void setGridY(int gridY) {
        this.gridY = gridY;
    }
    
    /**
     * このショートカットのグリッド位置を設定する。
     * 
     * @param gridX 新しいグリッドX座標（0ベース）
     * @param gridY 新しいグリッドY座標（0ベース）
     */
    public void setGridPosition(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
    }
    
    /**
     * このショートカットの表示名を取得する。
     * カスタム名が設定されている場合はそれを返し、そうでなければアプリケーション名を返す。
     * 
     * @return このショートカットに表示する名前
     */
    public String getDisplayName() {
        return customName != null ? customName : application.getName();
    }
    
    /**
     * このショートカットに設定されたカスタム表示名を取得する。
     * 
     * @return カスタム名、またはアプリケーションのデフォルト名を使用している場合null
     */
    public String getCustomName() {
        return customName;
    }
    
    /**
     * このショートカットにカスタム表示名を設定する。
     * 
     * @param customName カスタム名、またはアプリケーション名を使用する場合null
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    
    /**
     * このショートカットが現在ドラッグされているかどうかを確認する。
     * 
     * @return ショートカットがドラッグされている場合true、そうでなければfalse
     */
    public boolean isDragging() {
        return isDragging;
    }
    
    /**
     * このショートカットのドラッグ状態を設定する。
     * 
     * @param dragging ドラッグ中としてマークする場合true、そうでなければfalse
     */
    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }
    
    /**
     * 現在のドラッグX位置（画面座標）を取得する。
     * 
     * @return ドラッグX位置
     */
    public float getDragX() {
        return dragX;
    }
    
    /**
     * 現在のドラッグX位置（画面座標）を設定する。
     * 
     * @param dragX 新しいドラッグX位置
     */
    public void setDragX(float dragX) {
        this.dragX = dragX;
    }
    
    /**
     * 現在のドラッグY位置（画面座標）を取得する。
     * 
     * @return ドラッグY位置
     */
    public float getDragY() {
        return dragY;
    }
    
    /**
     * 現在のドラッグY位置（画面座標）を設定する。
     * 
     * @param dragY 新しいドラッグY位置
     */
    public void setDragY(float dragY) {
        this.dragY = dragY;
    }
    
    /**
     * ドラッグ位置（画面座標）を設定する。
     * 
     * @param dragX 新しいドラッグX位置
     * @param dragY 新しいドラッグY位置
     */
    public void setDragPosition(float dragX, float dragY) {
        this.dragX = dragX;
        this.dragY = dragY;
    }
    
    /**
     * 編集モード用の現在の搖れアニメーションオフセットを取得する。
     * 
     * @return 搖れオフセット値
     */
    public float getWiggleOffset() {
        return wiggleOffset;
    }
    
    /**
     * 編集モード用の搖れアニメーションオフセットを設定する。
     * 
     * @param wiggleOffset 新しい搖れオフセット値
     */
    public void setWiggleOffset(float wiggleOffset) {
        this.wiggleOffset = wiggleOffset;
    }
    
    /**
     * このショートカットの一意識別子を取得する。
     * 
     * @return ショートカットID
     */
    public String getShortcutId() {
        return shortcutId;
    }
    
    /**
     * このショートカットが他のショートカットと同じアプリケーションを参照しているかどうかを確認する。
     * 
     * @param other 比較する他のショートカット
     * @return 両方のショートカットが同じアプリケーションを参照している場合true
     */
    public boolean referencesApplication(Shortcut other) {
        if (other == null) return false;
        return application.getApplicationId().equals(other.application.getApplicationId());
    }
    
    /**
     * このショートカットが指定されたアプリケーションを参照しているかどうかを確認する。
     * 
     * @param app 確認するアプリケーション
     * @return このショートカットが指定されたアプリケーションを参照している場合true
     */
    public boolean referencesApplication(IApplication app) {
        if (app == null) return false;
        return application.getApplicationId().equals(app.getApplicationId());
    }
    
    /**
     * 同じアプリケーション参照を持ち、異なる位置やプロパティを持つ可能性のある
     * このショートカットのコピーを作成する。
     * 
     * @param newGridX コピー用のグリッドX位置
     * @param newGridY コピー用のグリッドY位置
     * @return 新しいShortcutインスタンス
     */
    public Shortcut createCopy(int newGridX, int newGridY) {
        Shortcut copy = new Shortcut(application, newGridX, newGridY);
        copy.setCustomName(customName);
        return copy;
    }
    
    /**
     * 常時表示フィールド（Dock）内の位置を取得する。
     *
     * @return Dock内の位置（0-3）、Dockにない場合は-1
     */
    public int getDockPosition() {
        return dockPosition;
    }

    /**
     * 常時表示フィールド（Dock）内の位置を設定する。
     *
     * @param dockPosition Dock内の位置（0-3）、Dockから削除する場合は-1
     */
    public void setDockPosition(int dockPosition) {
        this.dockPosition = dockPosition;
    }

    /**
     * このショートカットが常時表示フィールド（Dock）にあるかどうかを確認する。
     *
     * @return Dockにある場合true
     */
    public boolean isInDock() {
        return dockPosition >= 0;
    }

    /**
     * このショートカットの文字列表現を返す。
     *
     * @return このショートカットを説明する文字列
     */
    @Override
    public String toString() {
        return "Shortcut{" +
                "app=" + application.getName() +
                ", pos=(" + gridX + "," + gridY + ")" +
                ", dockPos=" + dockPosition +
                ", dragging=" + isDragging +
                ", id=" + shortcutId +
                '}';
    }
    
    /**
     * このショートカットが他のオブジェクトと等しいかどうかを確認する。
     * 2つのショートカットは同じショートカットIDを持つ場合に等しいとみなされる。
     * 
     * @param obj 比較するオブジェクト
     * @return オブジェクトが等しい場合true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shortcut shortcut = (Shortcut) obj;
        return shortcutId.equals(shortcut.shortcutId);
    }
    
    /**
     * このショートカットのハッシュコードを返す。
     * 
     * @return ショートカットIDに基づくハッシュコード
     */
    @Override
    public int hashCode() {
        return shortcutId.hashCode();
    }
}