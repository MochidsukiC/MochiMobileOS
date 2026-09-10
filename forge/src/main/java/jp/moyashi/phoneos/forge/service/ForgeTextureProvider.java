package jp.moyashi.phoneos.forge.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jp.moyashi.phoneos.core.service.chromium.texture.TextureProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Forge環境用のTextureProvider実装。
 * Minecraft.getInstance().getResourceManager()を使用してテクスチャPNGを取得する。
 * クライアント側のChromiumスレッドで実行されるため、Minecraft.getInstance()が使用可能。
 */
public class ForgeTextureProvider implements TextureProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    @Override
    public byte[] getItemTexture(String namespace, String path) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;

            ResourceManager rm = mc.getResourceManager();
            ResourceLocation itemId = new ResourceLocation(namespace, path);

            return resolveTexture(rm, itemId);
        } catch (Exception e) {
            LOGGER.debug("[ForgeTextureProvider] Failed to get texture for {}:{} - {}", namespace, path, e.getMessage());
            return null;
        }
    }

    private byte[] resolveTexture(ResourceManager rm, ResourceLocation itemId) {
        // 1. アイテムモデルJSONから探す
        ResourceLocation modelLoc = new ResourceLocation(
            itemId.getNamespace(), "models/item/" + itemId.getPath() + ".json"
        );
        ResourceLocation textureLoc = findTextureInModelRecursive(rm, modelLoc, 0);

        // 2. ブロックモデルJSONから探す
        if (textureLoc == null) {
            modelLoc = new ResourceLocation(
                itemId.getNamespace(), "models/block/" + itemId.getPath() + ".json"
            );
            textureLoc = findTextureInModelRecursive(rm, modelLoc, 0);
        }

        // 3. モデルから見つからない場合、テクスチャパスを直接推測
        if (textureLoc == null) {
            ResourceLocation guessLoc = new ResourceLocation(
                itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png"
            );
            byte[] png = loadPng(rm, guessLoc);
            if (png != null) return png;

            guessLoc = new ResourceLocation(
                itemId.getNamespace(), "textures/block/" + itemId.getPath() + ".png"
            );
            return loadPng(rm, guessLoc);
        }

        // 4. テクスチャパスからPNGを読み込む
        ResourceLocation fileLoc = new ResourceLocation(
            textureLoc.getNamespace(), "textures/" + textureLoc.getPath() + ".png"
        );
        return loadPng(rm, fileLoc);
    }

    private ResourceLocation findTextureInModelRecursive(ResourceManager rm, ResourceLocation modelLoc, int depth) {
        if (depth > 10) return null;

        try {
            Optional<Resource> res = rm.getResource(modelLoc);
            if (res.isEmpty()) return null;

            try (InputStreamReader reader = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                if (json.has("textures")) {
                    JsonObject textures = json.getAsJsonObject("textures");
                    if (textures.has("layer0")) return parseResourceLocation(textures.get("layer0").getAsString());
                    if (textures.has("all")) return parseResourceLocation(textures.get("all").getAsString());
                    if (textures.has("texture")) return parseResourceLocation(textures.get("texture").getAsString());
                    if (textures.has("particle") && !textures.get("particle").getAsString().startsWith("#")) {
                        return parseResourceLocation(textures.get("particle").getAsString());
                    }
                }

                if (json.has("parent")) {
                    String parentPath = json.get("parent").getAsString();
                    ResourceLocation parentLoc = parseResourceLocation(parentPath);
                    String p = parentLoc.getPath();
                    if (!p.startsWith("models/")) {
                        if (p.startsWith("builtin/")) return null;
                        p = "models/" + p + ".json";
                    }
                    ResourceLocation nextModelLoc = new ResourceLocation(parentLoc.getNamespace(), p);
                    return findTextureInModelRecursive(rm, nextModelLoc, depth + 1);
                }
            }
        } catch (Exception e) {
            // 無視
        }
        return null;
    }

    private byte[] loadPng(ResourceManager rm, ResourceLocation loc) {
        try {
            Optional<Resource> res = rm.getResource(loc);
            if (res.isPresent()) {
                try (InputStream is = res.get().open()) {
                    return is.readAllBytes();
                }
            }
        } catch (Exception e) {
            // 無視
        }
        return null;
    }

    private ResourceLocation parseResourceLocation(String path) {
        if (path.contains(":")) {
            return new ResourceLocation(path);
        } else {
            return new ResourceLocation("minecraft", path);
        }
    }
}
