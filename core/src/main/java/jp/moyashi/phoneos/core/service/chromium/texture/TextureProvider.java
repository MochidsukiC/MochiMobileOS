package jp.moyashi.phoneos.core.service.chromium.texture;

/**
 * アイテムテクスチャを提供するインターフェース。
 * coreモジュールはMinecraftクライアントに依存しないため、
 * テクスチャ取得ロジックはforge側で実装する。
 */
public interface TextureProvider {

    /**
     * アイテムIDからテクスチャPNGバイト配列を返す。
     *
     * @param namespace 名前空間（例: "minecraft"）
     * @param path アイテムパス（例: "stone"）
     * @return PNGバイト配列。見つからない場合はnull
     */
    byte[] getItemTexture(String namespace, String path);
}
