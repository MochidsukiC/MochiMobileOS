package jp.moyashi.phoneos.core.apps.voicememo;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PGraphics;

/**
 * ボイスメモアプリケーション。
 * マイクで録音し、スピーカーで再生する機能を提供する。
 *
 * 機能:
 * - 音声の録音（マイクAPI使用）
 * - 音声の再生（スピーカーAPI使用）
 * - メモの保存（VFS使用）
 * - メモ一覧表示
 */
public class VoiceMemoApp implements IApplication {
    private VoiceMemoScreen screen;

    @Override
    public String getName() {
        return "Voice Memo";
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        if (screen == null) {
            screen = new VoiceMemoScreen(kernel);
        }
        return screen;
    }

    @Override
    public void onDestroy() {
        // クリーンアップ処理
        if (screen != null) {
            screen.stopRecording();
            screen.stopPlayback();
        }
    }
}
