package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * ChromiumServiceのNoOp（何もしない）実装。
 * ChromiumProviderが利用できない環境で使用される。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class NoOpChromiumService implements ChromiumService {

    @Override
    public void initialize(Kernel kernel) throws Exception {
        // 何もしない
    }

    @Override
    public ChromiumSurface createTab(int width, int height, String initialUrl) {
        // nullを返すとエラーになる可能性があるため、NoOpなサーフェスを返す必要があるかも
        return null;
    }

    @Override
    public void closeTab(String surfaceId) {
        // 何もしない
    }

    @Override
    public Optional<ChromiumSurface> findSurface(String surfaceId) {
        return Optional.empty();
    }

    @Override
    public Optional<ChromiumSurface> getActiveSurface() {
        return Optional.empty();
    }

    @Override
    public void setActiveSurface(String surfaceId) {
        // 何もしない
    }

    @Override
    public Collection<ChromiumSurface> getSurfaces() {
        return Collections.emptyList();
    }

    @Override
    public void update() {
        // 何もしない
    }

    @Override
    public void shutdown() {
        // 何もしない
    }

    @Override
    public BrowserDataManager getBrowserDataManager() {
        // NoOp実装では常にnullを返す
        // BrowserDataManagerは具象クラスで、VFSに依存するため
        // NoOp実装では機能しない
        return null;
    }
}