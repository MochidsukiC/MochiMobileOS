package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.BatteryInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Forge環境用のバッテリー情報実装。
 * アイテムNBTからバッテリー残量とバッテリー寿命を取得する。
 */
public class ForgeBatteryInfo implements BatteryInfo {
    private ItemStack itemStack;
    private int batteryLevel;
    private int batteryHealth;
    private boolean charging;

    private static final String NBT_BATTERY_LEVEL = "BatteryLevel";
    private static final String NBT_BATTERY_HEALTH = "BatteryHealth";
    private static final String NBT_CHARGING = "Charging";

    public ForgeBatteryInfo(ItemStack itemStack) {
        this.itemStack = itemStack;
        loadFromNBT();
    }

    /**
     * NBTからバッテリー情報を読み込む。
     */
    private void loadFromNBT() {
        if (itemStack != null && itemStack.hasTag()) {
            CompoundTag tag = itemStack.getTag();
            this.batteryLevel = tag.getInt(NBT_BATTERY_LEVEL);
            this.batteryHealth = tag.getInt(NBT_BATTERY_HEALTH);
            this.charging = tag.getBoolean(NBT_CHARGING);

            // デフォルト値の設定
            if (this.batteryLevel == 0 && !tag.contains(NBT_BATTERY_LEVEL)) {
                this.batteryLevel = 100;
            }
            if (this.batteryHealth == 0 && !tag.contains(NBT_BATTERY_HEALTH)) {
                this.batteryHealth = 100;
            }
        } else {
            // NBTがない場合はデフォルト値
            this.batteryLevel = 100;
            this.batteryHealth = 100;
            this.charging = false;
        }
    }

    /**
     * NBTにバッテリー情報を保存する。
     */
    private void saveToNBT() {
        if (itemStack != null) {
            CompoundTag tag = itemStack.getOrCreateTag();
            tag.putInt(NBT_BATTERY_LEVEL, batteryLevel);
            tag.putInt(NBT_BATTERY_HEALTH, batteryHealth);
            tag.putBoolean(NBT_CHARGING, charging);
        }
    }

    /**
     * ItemStackを更新する（アイテムスロット変更時など）。
     */
    public void updateItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        loadFromNBT();
    }

    @Override
    public int getBatteryLevel() {
        // 最新のNBTから読み込む
        loadFromNBT();
        return batteryLevel;
    }

    @Override
    public int getBatteryHealth() {
        // 最新のNBTから読み込む
        loadFromNBT();
        return batteryHealth;
    }

    @Override
    public boolean isCharging() {
        // 最新のNBTから読み込む
        loadFromNBT();
        return charging;
    }

    @Override
    public void setBatteryLevel(int level) {
        this.batteryLevel = Math.max(0, Math.min(100, level));
        saveToNBT();
    }

    @Override
    public void setBatteryHealth(int health) {
        this.batteryHealth = Math.max(0, Math.min(100, health));
        saveToNBT();
    }

    @Override
    public void setCharging(boolean charging) {
        this.charging = charging;
        saveToNBT();
    }
}