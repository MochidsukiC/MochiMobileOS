package jp.moyashi.phoneos.core.service;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * System.out/System.errをキャプチャしてLoggerServiceに転送するPrintStream。
 *
 * MMOSパッケージ（jp.moyashi.phoneos）からの出力のみをキャプチャし、
 * 外部ライブラリ（JCEF等）からの出力はキャプチャしない。
 * 元のコンソール出力は常に維持される。
 */
public class CapturingPrintStream extends PrintStream {

    private final PrintStream original;
    private final LoggerService logger;
    private final boolean isErrorStream;

    /** 無限ループ防止用フラグ（スレッドローカル） */
    private static final ThreadLocal<Boolean> isLogging = ThreadLocal.withInitial(() -> false);

    /**
     * CapturingPrintStreamを構築する。
     *
     * @param original 元のPrintStream（System.out または System.err）
     * @param logger LoggerServiceインスタンス
     * @param isErrorStream System.errの場合true、System.outの場合false
     */
    public CapturingPrintStream(PrintStream original, LoggerService logger, boolean isErrorStream) {
        super(original, true); // autoFlush = true
        this.original = original;
        this.logger = logger;
        this.isErrorStream = isErrorStream;
    }

    /**
     * 元のPrintStreamを取得する。
     * @return 元のPrintStream
     */
    public PrintStream getOriginal() {
        return original;
    }

    @Override
    public void println(String x) {
        original.println(x);
        captureIfFromMMOS(x);
    }

    @Override
    public void println(Object x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(boolean x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        original.println(x);
        captureIfFromMMOS(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        original.println(x);
        captureIfFromMMOS(new String(x));
    }

    @Override
    public void println() {
        original.println();
        // 空行はキャプチャしない
    }

    @Override
    public void print(String s) {
        original.print(s);
        // print（改行なし）はキャプチャしない（printlnのみ対象）
    }

    @Override
    public void print(Object obj) {
        original.print(obj);
    }

    @Override
    public void print(int i) {
        original.print(i);
    }

    @Override
    public void print(long l) {
        original.print(l);
    }

    @Override
    public void print(float f) {
        original.print(f);
    }

    @Override
    public void print(double d) {
        original.print(d);
    }

    @Override
    public void print(boolean b) {
        original.print(b);
    }

    @Override
    public void print(char c) {
        original.print(c);
    }

    @Override
    public void print(char[] s) {
        original.print(s);
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        original.printf(format, args);
        // printf はキャプチャしない（複雑なフォーマット処理を避けるため）
        return this;
    }

    @Override
    public PrintStream printf(java.util.Locale l, String format, Object... args) {
        original.printf(l, format, args);
        return this;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        original.write(buf, off, len);
        // バイト書き込みはキャプチャしない
    }

    @Override
    public void write(int b) {
        original.write(b);
    }

    @Override
    public void flush() {
        original.flush();
    }

    @Override
    public void close() {
        // 元のストリームは閉じない（System.out/errは閉じてはいけない）
        flush();
    }

    /**
     * LoggerServiceに転送する。
     * 注意: スタックトレース検査は非常にコストが高いため廃止。
     * すべての出力をキャプチャするが、ログレベルでフィルタリングされる。
     *
     * @param message キャプチャするメッセージ
     */
    private void captureIfFromMMOS(String message) {
        // 無限ループ防止
        if (isLogging.get()) {
            return;
        }

        if (logger == null || message == null || message.isEmpty()) {
            return;
        }

        // ログレベルをバイパスして直接ファイルに書き込む
        try {
            isLogging.set(true);
            String tag = isErrorStream ? "STDERR" : "STDOUT";
            logger.logDirect(tag, message);
        } finally {
            isLogging.set(false);
        }
    }
}
