package jp.moyashi.phoneos.forge.item;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.forge.gui.ProcessingScreen;
import jp.moyashi.phoneos.forge.service.SmartphoneBackgroundService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

/**
 * スマートフォンアイテムクラス。
 * 右クリックでMochiMobileOSのProcessing画面を開くことができる。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class SmartphoneItem extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * スマートフォンアイテムのコンストラクタ。
     *
     * @param properties アイテムのプロパティ
     */
    public SmartphoneItem(Properties properties) {
        super(properties);
    }

    /**
     * ブロックに対して右クリックした時の処理。
     * IC通信が有効な場合、ブロックをスキャンしてGUIを開かない。
     *
     * @param context 使用コンテキスト
     * @return 結果
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos blockPos = context.getClickedPos();

        if (level.isClientSide && player != null) {
            // Kernelを取得してIC通信が有効か確認
            Kernel kernel = SmartphoneBackgroundService.getKernel();
            if (kernel != null && kernel.getICSocket() != null) {
                if (kernel.getICSocket().isEnabled()) {
                    // IC通信が有効：ブロックをスキャン
                    LOGGER.info("[SmartphoneItem] IC scan on block at " + blockPos);

                    if (kernel.getICSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeICSocket) {
                        ((jp.moyashi.phoneos.forge.hardware.ForgeICSocket) kernel.getICSocket())
                            .onBlockScanned(blockPos);
                    }

                    // GUIを開かずに終了
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // IC通信が無効な場合はブロックに対する通常の処理（何もしない）
        return InteractionResult.PASS;
    }

    /**
     * エンティティに対して右クリックした時の処理。
     * IC通信が有効な場合、エンティティをスキャンしてGUIを開かない。
     *
     * @param stack アイテムスタック
     * @param player プレイヤー
     * @param entity 対象エンティティ
     * @param hand 使用した手
     * @return 結果
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity entity, InteractionHand hand) {
        Level level = player.level();

        if (level.isClientSide) {
            // Kernelを取得してIC通信が有効か確認
            Kernel kernel = SmartphoneBackgroundService.getKernel();
            if (kernel != null && kernel.getICSocket() != null) {
                if (kernel.getICSocket().isEnabled()) {
                    // IC通信が有効：エンティティをスキャン
                    LOGGER.info("[SmartphoneItem] IC scan on entity " + entity.getUUID());

                    if (kernel.getICSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeICSocket) {
                        ((jp.moyashi.phoneos.forge.hardware.ForgeICSocket) kernel.getICSocket())
                            .onEntityScanned(entity);
                    }

                    // GUIを開かずに終了
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // IC通信が無効な場合は通常の処理
        return InteractionResult.PASS;
    }

    /**
     * アイテムが右クリックされた時の処理（空中での右クリック）。
     * IC通信が無効な場合のみGUIを開く。
     *
     * @param level ワールド
     * @param player プレイヤー
     * @param hand 使用した手
     * @return 結果
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        LOGGER.info("[SmartphoneItem] use() called - level.isClientSide: " + level.isClientSide +
                          ", player: " + player.getName().getString());

        if (level.isClientSide) {
            LOGGER.info("[SmartphoneItem] Client side - opening GUI");
            // クライアントサイドでのみGUIを開く
            openSmartphoneGUI(player);
        } else {
            LOGGER.info("[SmartphoneItem] Server side - skipping GUI");
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    /**
     * スマートフォンGUIを開く処理。
     * クライアントサイドでのみ実行される。
     *
     * @param player プレイヤー
     */
    @OnlyIn(Dist.CLIENT)
    private void openSmartphoneGUI(Player player) {
        try {
            LOGGER.info("[SmartphoneItem] ==================== OPENING SMARTPHONE GUI ====================");
            LOGGER.info("[SmartphoneItem] Player: " + player.getName().getString());
            LOGGER.info("[SmartphoneItem] Player UUID: " + player.getUUID());

            // MinecraftのGUIシステムを使ってProcessingベースのMochiMobileOS画面を開く
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("[SmartphoneItem] Creating ProcessingScreen instance...");

            ProcessingScreen processingScreen = new ProcessingScreen();
            LOGGER.info("[SmartphoneItem] ProcessingScreen created, setting screen...");

            minecraft.setScreen(processingScreen);
            LOGGER.info("[SmartphoneItem] Screen set successfully");

            // 現在の画面サイズを確認
            LOGGER.info("[SmartphoneItem] Minecraft window size: " +
                minecraft.getWindow().getGuiScaledWidth() + "x" +
                minecraft.getWindow().getGuiScaledHeight());

            LOGGER.info("[SmartphoneItem] ==================== GUI OPENING COMPLETE ====================");
        } catch (Exception e) {
            LOGGER.error("[SmartphoneItem] ==================== GUI OPENING FAILED ====================");
            LOGGER.error("[SmartphoneItem] Error: " + e.getMessage(), e);
        }
    }

    /**
     * アイテムのツールチップに表示される説明文を設定する。
     *
     * @return ツールチップのテキスト
     */
    @Override
    public String getDescriptionId() {
        return "item.mochimobileos.smartphone";
    }

    /**
     * アイテムが壊れるかどうかを設定。
     * スマートフォンは壊れない。
     *
     * @param stack アイテムスタック
     * @return 壊れるかどうか
     */
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    // getMaxStackSize()はfinalメソッドなのでオーバーライドできません
    // アイテムPropertiesのstacksTo(1)で設定済み
}