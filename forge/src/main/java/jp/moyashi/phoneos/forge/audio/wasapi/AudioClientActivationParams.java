package jp.moyashi.phoneos.forge.audio.wasapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

import java.util.Arrays;
import java.util.List;

/**
 * AUDIOCLIENT_ACTIVATION_PARAMS構造体のJNA表現。
 * Process-specific audio loopback captureに使用する。
 *
 * typedef struct AUDIOCLIENT_ACTIVATION_PARAMS {
 *     AUDIOCLIENT_ACTIVATION_TYPE ActivationType;
 *     union {
 *         AUDIOCLIENT_PROCESS_LOOPBACK_PARAMS ProcessLoopbackParams;
 *     };
 * } AUDIOCLIENT_ACTIVATION_PARAMS;
 */
@Structure.FieldOrder({"ActivationType", "ProcessLoopbackParams"})
public class AudioClientActivationParams extends Structure {
    public int ActivationType;
    public ProcessLoopbackParams ProcessLoopbackParams;

    public AudioClientActivationParams() {
        super();
    }

    /**
     * Process Loopback用のパラメータを作成する。
     *
     * @param targetProcessId キャプチャ対象のプロセスID
     * @param includeProcessTree trueの場合、プロセスツリー全体をキャプチャ
     * @return 設定済みのパラメータ
     */
    public static AudioClientActivationParams createProcessLoopback(int targetProcessId, boolean includeProcessTree) {
        AudioClientActivationParams params = new AudioClientActivationParams();
        params.ActivationType = WasapiConstants.AUDIOCLIENT_ACTIVATION_TYPE_PROCESS_LOOPBACK;
        params.ProcessLoopbackParams = new ProcessLoopbackParams();
        params.ProcessLoopbackParams.TargetProcessId = targetProcessId;
        params.ProcessLoopbackParams.ProcessLoopbackMode = includeProcessTree
            ? WasapiConstants.PROCESS_LOOPBACK_MODE_INCLUDE_TARGET_PROCESS_TREE
            : WasapiConstants.PROCESS_LOOPBACK_MODE_EXCLUDE_TARGET_PROCESS_TREE;
        return params;
    }

    /**
     * AUDIOCLIENT_PROCESS_LOOPBACK_PARAMS構造体。
     */
    @Structure.FieldOrder({"TargetProcessId", "ProcessLoopbackMode"})
    public static class ProcessLoopbackParams extends Structure {
        public int TargetProcessId;
        public int ProcessLoopbackMode;

        public ProcessLoopbackParams() {
            super();
        }

        public static class ByReference extends ProcessLoopbackParams implements Structure.ByReference {
        }
    }

    public static class ByReference extends AudioClientActivationParams implements Structure.ByReference {
    }
}
