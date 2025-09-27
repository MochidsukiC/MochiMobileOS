package jp.moyashi.phoneos.forge.registry;

import jp.moyashi.phoneos.forge.item.SmartphoneItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MochiMobileOS MODのアイテム登録クラス。
 * MODで追加される全てのアイテムを管理する。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class ModItems {

    /** MOD ID */
    public static final String MODID = "mochimobileos";

    /** アイテム登録用のDeferredRegister */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /**
     * スマートフォンアイテムの登録。
     * 右クリックでMochiMobileOSの画面を開くことができる。
     */
    public static final RegistryObject<Item> SMARTPHONE = ITEMS.register("smartphone",
        () -> new SmartphoneItem(new Item.Properties()
            .stacksTo(1)  // スタック数は1個まで
            .rarity(Rarity.RARE)  // レア度を設定
            // クリエイティブタブは別途イベントで設定
        )
    );

    /**
     * アイテム登録をイベントバスに登録する。
     *
     * @param eventBus MODのイベントバス
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        System.out.println("[ModItems] Items registered to event bus");
    }

    /**
     * 登録済みアイテムの初期化処理。
     * アイテムが正常に登録されたかをログで確認する。
     */
    public static void initialize() {
        System.out.println("[ModItems] Initializing mod items...");
        System.out.println("[ModItems] Smartphone item: " + (SMARTPHONE.get() != null ? "OK" : "FAILED"));
        System.out.println("[ModItems] All mod items initialized");
    }
}