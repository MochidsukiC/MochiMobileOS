package jp.moyashi.phoneos.core.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * スマートフォンOS用のシステムクロックサービス。
 * このクラスはオペレーティングシステムのための時刻と日付機能を提供する。
 * システム時刻、フォーマット、時刻関連の操作を扱う。
 * 
 * @author YourName
 * @version 1.0
 */
public class SystemClock {
    
    /** HH:mm形式で時刻を表示するためのフォーマッター */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    
    /** yyyy-MM-dd形式で日付を表示するためのフォーマッター */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /** 日付と時刻の完全形式を表示するためのフォーマッター */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /** 稼働時間計算のためのシステム開始時刻 */
    private final LocalDateTime systemStartTime;
    
    /**
     * Constructs a new SystemClock instance.
     * Records the system start time for uptime tracking.
     */
    public SystemClock() {
        systemStartTime = LocalDateTime.now();
        System.out.println("SystemClock: System clock initialized at " + getFormattedDateTime());
    }
    
    /**
     * 現在のシステム時刻を取得する。
     * 
     * @return 現在のLocalDateTime
     */
    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
    
    /**
     * HH:mm形式でフォーマットされた現在時刻を取得する。
     * 
     * @return フォーマットされた時刻文字列
     */
    public String getFormattedTime() {
        return getCurrentTime().format(TIME_FORMAT);
    }
    
    /**
     * yyyy-MM-dd形式でフォーマットされた現在日付を取得する。
     * 
     * @return フォーマットされた日付文字列
     */
    public String getFormattedDate() {
        return getCurrentTime().format(DATE_FORMAT);
    }
    
    /**
     * yyyy-MM-dd HH:mm:ss形式でフォーマットされた現在日時を取得する。
     * 
     * @return フォーマットされた日時文字列
     */
    public String getFormattedDateTime() {
        return getCurrentTime().format(DATETIME_FORMAT);
    }
    
    /**
     * システム開始時刻を取得する。
     * 
     * @return システムが開始されたときのLocalDateTime
     */
    public LocalDateTime getSystemStartTime() {
        return systemStartTime;
    }
    
    /**
     * エポック時刻からのミリ秒でシステム開始時刻を取得する。
     * 
     * @return ミリ秒単位のシステム開始時刻
     */
    public long getStartTime() {
        return java.time.Instant.from(systemStartTime.atZone(java.time.ZoneId.systemDefault())).toEpochMilli();
    }
    
    /**
     * ミリ秒単位でシステム稼働時間を計算する。
     * 
     * @return ミリ秒単位のシステム稼働時間
     */
    public long getUptimeMillis() {
        return java.time.Duration.between(systemStartTime, getCurrentTime()).toMillis();
    }
    
    /**
     * 時間:分:秒形式でフォーマットされた稼働時間文字列を取得する。
     * 
     * @return フォーマットされた稼働時間文字列
     */
    public String getFormattedUptime() {
        long uptimeMs = getUptimeMillis();
        long hours = uptimeMs / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (uptimeMs % (1000 * 60)) / 1000;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    /**
     * 現在時刻が営業時間内（午前9時から午後5時）かどうかを確認する。
     * 
     * @return 現在時刻が営業時間内の場合true、そうでなければfalse
     */
    public boolean isBusinessHours() {
        int currentHour = getCurrentTime().getHour();
        return currentHour >= 9 && currentHour < 17;
    }
}