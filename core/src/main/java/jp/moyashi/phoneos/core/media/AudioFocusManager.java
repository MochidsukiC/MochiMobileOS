package jp.moyashi.phoneos.core.media;

import java.util.Stack;
import java.util.logging.Logger;

/**
 * 音声出力の排他制御を管理するサービス。
 * 複数のアプリケーション間での音声競合を解決する。
 *
 * AudioFocusManagerは、音声を再生しようとするアプリケーションが
 * フォーカスを要求し、他のアプリケーションにフォーカスの喪失を通知する
 * メカニズムを提供する。
 *
 * 使用例:
 * <pre>
 * AudioFocusManager.FocusResult result = audioFocusManager.requestFocus(
 *     appId,
 *     focusChange -> {
 *         switch (focusChange) {
 *             case LOSS:
 *                 stopPlayback();
 *                 break;
 *             case LOSS_TRANSIENT:
 *                 pausePlayback();
 *                 break;
 *             case LOSS_TRANSIENT_CAN_DUCK:
 *                 lowerVolume();
 *                 break;
 *             case GAIN:
 *                 resumePlayback();
 *                 break;
 *         }
 *     },
 *     false // isDuckable
 * );
 * </pre>
 *
 * @since 2025-12-21
 * @version 1.0
 */
public class AudioFocusManager {

    private static final Logger logger = Logger.getLogger(AudioFocusManager.class.getName());

    /**
     * フォーカス要求の結果。
     */
    public enum FocusResult {
        /** フォーカス取得成功 */
        GRANTED,
        /** フォーカスは後で付与される（遅延付与） */
        DELAYED,
        /** フォーカス取得失敗 */
        FAILED
    }

    /**
     * フォーカス変更通知の種類。
     */
    public enum FocusChange {
        /** フォーカス獲得 */
        GAIN,
        /** フォーカス完全喪失（リソースを解放すべき） */
        LOSS,
        /** 一時的なフォーカス喪失（すぐに戻る可能性あり） */
        LOSS_TRANSIENT,
        /** ダッキング（音量を下げる）可能な一時的喪失 */
        LOSS_TRANSIENT_CAN_DUCK
    }

    /**
     * フォーカス変更を受け取るリスナー。
     */
    public interface AudioFocusListener {
        /**
         * フォーカスの状態が変化したときに呼び出される。
         *
         * @param change フォーカス変更の種類
         */
        void onAudioFocusChange(FocusChange change);
    }

    /**
     * フォーカス要求の情報を保持する内部クラス。
     */
    private static class FocusRequest {
        final String applicationId;
        final AudioFocusListener listener;
        final boolean isDuckable;

        FocusRequest(String applicationId, AudioFocusListener listener, boolean isDuckable) {
            this.applicationId = applicationId;
            this.listener = listener;
            this.isDuckable = isDuckable;
        }
    }

    /** フォーカス履歴のスタック（一時的に失われたフォーカスを復元するため） */
    private final Stack<FocusRequest> focusStack = new Stack<>();

    /** 現在のフォーカス保持者 */
    private FocusRequest currentFocusHolder = null;

    /**
     * AudioFocusManagerを作成する。
     */
    public AudioFocusManager() {
        logger.info("AudioFocusManager initialized");
    }

    /**
     * オーディオフォーカスを要求する。
     *
     * @param applicationId 要求元アプリケーションのID
     * @param listener フォーカス変更を受け取るリスナー
     * @param isDuckable このアプリケーションの音声がダッキング（音量低下）可能かどうか
     * @return フォーカス要求の結果
     */
    public synchronized FocusResult requestFocus(
            String applicationId,
            AudioFocusListener listener,
            boolean isDuckable) {

        if (applicationId == null || listener == null) {
            logger.warning("requestFocus: applicationId or listener is null");
            return FocusResult.FAILED;
        }

        FocusRequest request = new FocusRequest(applicationId, listener, isDuckable);

        // 既存のフォーカス保持者がいる場合
        if (currentFocusHolder != null) {
            // 同じアプリケーションが既にフォーカスを持っている場合
            if (currentFocusHolder.applicationId.equals(applicationId)) {
                logger.fine("requestFocus: " + applicationId + " already has focus");
                return FocusResult.GRANTED;
            }

            // 他のアプリケーションがフォーカスを持っている場合
            if (currentFocusHolder.isDuckable) {
                // ダッキング可能なら一時的喪失を通知してスタックに保存
                logger.fine("requestFocus: notifying " + currentFocusHolder.applicationId +
                        " of LOSS_TRANSIENT_CAN_DUCK");
                currentFocusHolder.listener.onAudioFocusChange(FocusChange.LOSS_TRANSIENT_CAN_DUCK);
                focusStack.push(currentFocusHolder);
            } else {
                // ダッキング不可なら完全喪失を通知
                logger.fine("requestFocus: notifying " + currentFocusHolder.applicationId +
                        " of LOSS");
                currentFocusHolder.listener.onAudioFocusChange(FocusChange.LOSS);
            }
        }

        // 新しいフォーカス保持者を設定
        currentFocusHolder = request;
        listener.onAudioFocusChange(FocusChange.GAIN);

        logger.info("AudioFocusManager: Focus granted to " + applicationId);
        return FocusResult.GRANTED;
    }

    /**
     * オーディオフォーカスを解放する。
     *
     * @param applicationId 解放するアプリケーションのID
     */
    public synchronized void abandonFocus(String applicationId) {
        if (applicationId == null) {
            return;
        }

        // 現在のフォーカス保持者の場合
        if (currentFocusHolder != null &&
                currentFocusHolder.applicationId.equals(applicationId)) {

            logger.info("AudioFocusManager: Focus abandoned by " + applicationId);
            currentFocusHolder = null;

            // スタックから前のフォーカス保持者を復元
            if (!focusStack.isEmpty()) {
                currentFocusHolder = focusStack.pop();
                currentFocusHolder.listener.onAudioFocusChange(FocusChange.GAIN);
                logger.fine("AudioFocusManager: Focus restored to " +
                        currentFocusHolder.applicationId);
            }
        } else {
            // スタック内にある場合は削除
            focusStack.removeIf(req -> req.applicationId.equals(applicationId));
            logger.fine("AudioFocusManager: Removed " + applicationId + " from focus stack");
        }
    }

    /**
     * 現在のフォーカス保持者のアプリケーションIDを取得する。
     *
     * @return アプリケーションID、フォーカス保持者がいない場合はnull
     */
    public synchronized String getCurrentFocusHolder() {
        return currentFocusHolder != null ? currentFocusHolder.applicationId : null;
    }

    /**
     * フォーカスを持っているかどうかを確認する。
     *
     * @param applicationId 確認するアプリケーションのID
     * @return フォーカスを持っている場合true
     */
    public synchronized boolean hasFocus(String applicationId) {
        return currentFocusHolder != null &&
                currentFocusHolder.applicationId.equals(applicationId);
    }

    /**
     * 一時的なフォーカス喪失を要求する。
     * 短い通知音などで使用する。
     *
     * @param applicationId 要求元アプリケーションのID
     * @param listener フォーカス変更を受け取るリスナー
     * @return フォーカス要求の結果
     */
    public synchronized FocusResult requestTransientFocus(
            String applicationId,
            AudioFocusListener listener) {

        if (currentFocusHolder != null &&
                !currentFocusHolder.applicationId.equals(applicationId)) {
            // 現在のフォーカス保持者に一時的喪失を通知
            currentFocusHolder.listener.onAudioFocusChange(FocusChange.LOSS_TRANSIENT);
            focusStack.push(currentFocusHolder);
        }

        FocusRequest request = new FocusRequest(applicationId, listener, false);
        currentFocusHolder = request;
        listener.onAudioFocusChange(FocusChange.GAIN);

        logger.fine("AudioFocusManager: Transient focus granted to " + applicationId);
        return FocusResult.GRANTED;
    }

    /**
     * フォーカススタックをクリアする。
     * システムリセット時などに使用。
     */
    public synchronized void clearFocusStack() {
        focusStack.clear();
        if (currentFocusHolder != null) {
            currentFocusHolder.listener.onAudioFocusChange(FocusChange.LOSS);
            currentFocusHolder = null;
        }
        logger.info("AudioFocusManager: Focus stack cleared");
    }
}
