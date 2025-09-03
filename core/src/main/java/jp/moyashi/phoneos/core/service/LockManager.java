package jp.moyashi.phoneos.core.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * OSのロック状態を管理するサービスクラス。
 * ロック画面の表示/非表示、パターン認証、ロック状態の保持を担当する。
 * SettingsManagerと連携して、ロック状態とパターンの永続化を行う。
 * 
 * このクラスの主要責務:
 * - OSのロック状態管理（ロック/アンロック）
 * - パターン認証処理
 * - ロックパターンの保存と読み込み
 * - セキュリティ関連の状態管理
 * 
 * @author YourName
 * @version 1.0
 */
public class LockManager {
    
    /** 設定管理サービスへの参照 */
    private SettingsManager settingsManager;
    
    /** 現在のロック状態（true: ロック中, false: アンロック中） */
    private boolean isLocked;
    
    /** 設定保存キー: ロック状態 */
    private static final String SETTING_LOCK_STATE = "system_lock_enabled";
    
    /** 設定保存キー: ロックパターン */
    private static final String SETTING_LOCK_PATTERN = "system_lock_pattern";
    
    /** デフォルトロックパターン（初期設定用: L字型パターン 0-3-6-7-8） */
    private static final List<Integer> DEFAULT_PATTERN = Arrays.asList(0, 3, 6, 7, 8);
    
    /**
     * LockManagerの新しいインスタンスを構築する。
     * SettingsManagerを受け取り、保存されたロック状態とパターンを読み込む。
     * 
     * @param settingsManager 設定管理サービスのインスタンス
     */
    public LockManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        
        // 保存されたロック状態を読み込む
        this.isLocked = settingsManager.getBooleanSetting(SETTING_LOCK_STATE, true); // デフォルトはロック状態
        
        // 初回起動時にデフォルトパターンを設定
        initializeDefaultPatternIfNeeded();
        
        System.out.println("LockManager: ロック管理サービスを初期化完了");
        System.out.println("LockManager: 現在のロック状態 = " + (isLocked ? "ロック中" : "アンロック中"));
    }
    
    /**
     * OSをアンロック状態にする。
     * ロック状態をfalseに設定し、設定を永続化する。
     */
    public void unlock() {
        this.isLocked = false;
        settingsManager.setSetting(SETTING_LOCK_STATE, false);
        settingsManager.saveSettings();
        System.out.println("LockManager: OSをアンロック状態に変更しました");
    }
    
    /**
     * OSをロック状態にする。
     * ロック状態をtrueに設定し、設定を永続化する。
     */
    public void lock() {
        this.isLocked = true;
        settingsManager.setSetting(SETTING_LOCK_STATE, true);
        settingsManager.saveSettings();
        System.out.println("LockManager: OSをロック状態に変更しました");
    }
    
    /**
     * 現在のロック状態を取得する。
     * 
     * @return true: ロック中, false: アンロック中
     */
    public boolean isLocked() {
        return isLocked;
    }
    
    /**
     * 入力されたパターンが正しいロックパターンと一致するかを検証する。
     * 
     * @param inputPattern 入力されたパターン（3x3グリッドのドット番号のリスト）
     * @return 正しいパターンの場合true、間違っている場合false
     */
    public boolean checkPattern(List<Integer> inputPattern) {
        if (inputPattern == null || inputPattern.isEmpty()) {
            System.out.println("LockManager: 空のパターンが入力されました");
            return false;
        }
        
        List<Integer> savedPattern = getSavedPattern();
        boolean isMatch = inputPattern.equals(savedPattern);
        
        System.out.println("LockManager: パターン認証結果 = " + (isMatch ? "成功" : "失敗"));
        System.out.println("LockManager: 入力パターン = " + inputPattern);
        System.out.println("LockManager: 正解パターン = " + savedPattern);
        
        return isMatch;
    }
    
    /**
     * 新しいロックパターンを保存する。
     * パターンをSettingsManager経由で永続化する。
     * 
     * @param pattern 保存する新しいパターン
     */
    public void savePattern(List<Integer> pattern) {
        if (pattern == null || pattern.isEmpty()) {
            System.out.println("LockManager: 無効なパターンが指定されました");
            return;
        }
        
        // パターンを文字列形式で保存（例: "0,3,6,7,8"）
        StringBuilder patternStr = new StringBuilder();
        for (int i = 0; i < pattern.size(); i++) {
            if (i > 0) patternStr.append(",");
            patternStr.append(pattern.get(i));
        }
        
        settingsManager.setSetting(SETTING_LOCK_PATTERN, patternStr.toString());
        settingsManager.saveSettings();
        
        System.out.println("LockManager: 新しいロックパターンを保存しました = " + pattern);
    }
    
    /**
     * 保存されているロックパターンを取得する。
     * 
     * @return 保存されているロックパターンのリスト
     */
    public List<Integer> getSavedPattern() {
        String patternStr = settingsManager.getStringSetting(SETTING_LOCK_PATTERN, "");
        
        if (patternStr.isEmpty()) {
            // パターンが設定されていない場合はデフォルトパターンを返す
            return new ArrayList<>(DEFAULT_PATTERN);
        }
        
        // 文字列からパターンを復元
        List<Integer> pattern = new ArrayList<>();
        String[] parts = patternStr.split(",");
        for (String part : parts) {
            try {
                pattern.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                System.err.println("LockManager: パターン復元エラー: " + e.getMessage());
                return new ArrayList<>(DEFAULT_PATTERN);
            }
        }
        
        return pattern;
    }
    
    /**
     * デフォルトパターンが未設定の場合に初期化する。
     * 初回起動時にL字型のデフォルトパターンを設定する。
     */
    private void initializeDefaultPatternIfNeeded() {
        String existingPattern = settingsManager.getStringSetting(SETTING_LOCK_PATTERN, "");
        
        if (existingPattern.isEmpty()) {
            System.out.println("LockManager: デフォルトパターンを初期化中...");
            savePattern(DEFAULT_PATTERN);
            System.out.println("LockManager: L字型パターン (0-3-6-7-8) を設定しました");
        } else {
            System.out.println("LockManager: 既存のパターンを読み込みました");
        }
    }
    
    /**
     * ロックパターンが設定されているかどうかを確認する。
     * 
     * @return パターンが設定されている場合true
     */
    public boolean hasPattern() {
        String patternStr = settingsManager.getStringSetting(SETTING_LOCK_PATTERN, "");
        return !patternStr.isEmpty();
    }
    
    /**
     * デフォルトパターンの説明を取得する。
     * UIでパターン説明を表示する際に使用する。
     * 
     * @return デフォルトパターンの説明文字列
     */
    public String getDefaultPatternDescription() {
        return "L字型パターン (左上から下へ、右下へ)";
    }
    
    /**
     * パターンの入力試行回数を管理する。
     * セキュリティ機能として将来的に試行回数制限を実装する際に使用。
     * 現在は基本実装のみ提供。
     * 
     * @return 常に無制限（-1）
     */
    public int getRemainingAttempts() {
        // 将来の拡張用: 現在は無制限
        return -1;
    }
}