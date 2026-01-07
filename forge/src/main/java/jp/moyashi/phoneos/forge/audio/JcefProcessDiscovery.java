package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JCEFのjcef_helperプロセスを発見するユーティリティクラス。
 *
 * JCEFはマルチプロセスアーキテクチャを採用しており、
 * ブラウザのレンダリングは別プロセス（jcef_helper.exe）で実行される。
 * このクラスは現在のJVMプロセスの子プロセスからjcef_helperを特定する。
 *
 * 用途: WASAPI Process-Specific Captureでブラウザ音声のみをキャプチャするため。
 */
public class JcefProcessDiscovery {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String JCEF_HELPER_NAME = "jcef_helper";

    /**
     * 現在のJVMプロセスの子プロセスからjcef_helperプロセスを検索する。
     *
     * @return jcef_helperプロセスのPIDリスト（複数のブラウザタブがある場合は複数）
     */
    public static List<Long> findJcefHelperProcesses() {
        List<Long> jcefHelperPids = new ArrayList<>();

        try {
            // 現在のJVMプロセスを取得
            ProcessHandle currentProcess = ProcessHandle.current();
            long currentPid = currentProcess.pid();
            LOGGER.info("[JcefProcessDiscovery] Current JVM PID: {}", currentPid);

            // 子プロセスを再帰的に検索
            findJcefHelperRecursive(currentProcess, jcefHelperPids);

            if (jcefHelperPids.isEmpty()) {
                LOGGER.info("[JcefProcessDiscovery] No jcef_helper processes found");
            } else {
                LOGGER.info("[JcefProcessDiscovery] Found {} jcef_helper process(es): {}",
                    jcefHelperPids.size(), jcefHelperPids);
            }

        } catch (Exception e) {
            LOGGER.error("[JcefProcessDiscovery] Error finding jcef_helper processes", e);
        }

        return jcefHelperPids;
    }

    /**
     * 再帰的にプロセスツリーを探索してjcef_helperを見つける。
     */
    private static void findJcefHelperRecursive(ProcessHandle parent, List<Long> results) {
        parent.children().forEach(child -> {
            Optional<String> command = child.info().command();

            if (command.isPresent()) {
                String cmd = command.get().toLowerCase();

                // jcef_helper または jcef_helper.exe を検索
                if (cmd.contains(JCEF_HELPER_NAME)) {
                    long pid = child.pid();
                    results.add(pid);
                    LOGGER.debug("[JcefProcessDiscovery] Found jcef_helper: PID={}, command={}",
                        pid, command.get());
                }
            }

            // 子プロセスも再帰的に検索
            findJcefHelperRecursive(child, results);
        });
    }

    /**
     * jcef_helperプロセスが存在するか確認する。
     *
     * @return jcef_helperが存在する場合true
     */
    public static boolean hasJcefHelperProcess() {
        return !findJcefHelperProcesses().isEmpty();
    }

    /**
     * 最初に見つかったjcef_helperプロセスのPIDを取得する。
     * WASAPI Process-Specific Captureでは、includetreeモードを使用するため、
     * 1つのPIDを指定すればそのプロセスツリー全体の音声をキャプチャできる。
     *
     * @return jcef_helperのPID、見つからない場合は-1
     */
    public static long getFirstJcefHelperPid() {
        List<Long> pids = findJcefHelperProcesses();
        return pids.isEmpty() ? -1 : pids.get(0);
    }

    /**
     * 全てのプロセスからjcef_helperを検索する（デバッグ用）。
     * 通常は現在のJVMの子プロセスのみを検索するが、
     * デバッグ目的でシステム全体を検索する場合に使用。
     *
     * @return システム全体で見つかったjcef_helperプロセスのPIDリスト
     */
    public static List<Long> findAllJcefHelperProcesses() {
        List<Long> jcefHelperPids = new ArrayList<>();

        try {
            ProcessHandle.allProcesses().forEach(process -> {
                Optional<String> command = process.info().command();

                if (command.isPresent()) {
                    String cmd = command.get().toLowerCase();

                    if (cmd.contains(JCEF_HELPER_NAME)) {
                        long pid = process.pid();
                        jcefHelperPids.add(pid);

                        // 親プロセス情報も取得
                        Optional<ProcessHandle> parent = process.parent();
                        String parentInfo = parent.map(p -> "PID=" + p.pid()).orElse("none");

                        LOGGER.debug("[JcefProcessDiscovery] Found jcef_helper (system-wide): PID={}, parent={}, command={}",
                            pid, parentInfo, command.get());
                    }
                }
            });

            LOGGER.info("[JcefProcessDiscovery] System-wide search found {} jcef_helper process(es)",
                jcefHelperPids.size());

        } catch (Exception e) {
            LOGGER.error("[JcefProcessDiscovery] Error in system-wide search", e);
        }

        return jcefHelperPids;
    }

    /**
     * プロセスが存在し、まだ実行中かどうかを確認する。
     *
     * @param pid プロセスID
     * @return プロセスが存在し実行中の場合true
     */
    public static boolean isProcessAlive(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /**
     * 指定されたPIDのプロセス情報を取得する（デバッグ用）。
     *
     * @param pid プロセスID
     * @return プロセス情報の文字列表現
     */
    public static String getProcessInfo(long pid) {
        return ProcessHandle.of(pid)
            .map(p -> {
                ProcessHandle.Info info = p.info();
                return String.format("PID=%d, command=%s, user=%s, startTime=%s",
                    pid,
                    info.command().orElse("unknown"),
                    info.user().orElse("unknown"),
                    info.startInstant().map(Object::toString).orElse("unknown")
                );
            })
            .orElse("Process not found: " + pid);
    }
}
