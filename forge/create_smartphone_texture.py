#!/usr/bin/env python3
"""
スマートフォンアイテム用のMinecraftテクスチャを生成するスクリプト
16x16ピクセルのPNG画像を作成します
"""

from PIL import Image, ImageDraw
import os

def create_smartphone_texture():
    """スマートフォンのテクスチャを作成"""
    # 16x16のRGBA画像を作成
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # カラーパレット
    black = (0, 0, 0, 255)           # フレーム
    dark_gray = (64, 64, 64, 255)    # フレームシャドウ
    screen_black = (16, 16, 16, 255) # 画面オフ状態
    screen_blue = (100, 150, 255, 255) # 画面オン状態
    button_gray = (128, 128, 128, 255) # ホームボタン
    light_gray = (192, 192, 192, 255) # ハイライト

    # 背景は透明のまま

    # スマートフォンの外枠（黒フレーム）
    # 上部フレーム
    for x in range(3, 13):
        img.putpixel((x, 1), black)
        img.putpixel((x, 14), black)

    # 左右フレーム
    for y in range(2, 14):
        img.putpixel((3, y), black)
        img.putpixel((12, y), black)

    # 角を丸くするためのピクセル削除
    img.putpixel((3, 1), (0, 0, 0, 0))
    img.putpixel((12, 1), (0, 0, 0, 0))
    img.putpixel((3, 14), (0, 0, 0, 0))
    img.putpixel((12, 14), (0, 0, 0, 0))

    # 画面エリア（青いピクセルで画面オン状態を表現）
    for x in range(4, 12):
        for y in range(2, 13):
            if y < 10:  # 上部は明るい青（画面オン状態）
                img.putpixel((x, y), screen_blue)
            else:  # 下部は暗い部分（ベゼル）
                img.putpixel((x, y), dark_gray)

    # ホームボタン
    img.putpixel((7, 12), button_gray)
    img.putpixel((8, 12), button_gray)

    # ハイライト効果（画面の反射）
    img.putpixel((5, 3), light_gray)
    img.putpixel((6, 3), light_gray)
    img.putpixel((5, 4), light_gray)

    # スピーカー（上部の小さな線）
    for x in range(6, 10):
        img.putpixel((x, 2), dark_gray)

    return img

def main():
    """メイン関数"""
    print("Creating smartphone texture...")

    # テクスチャを作成
    texture = create_smartphone_texture()

    # 保存先パス
    output_path = "forge/src/main/resources/assets/mochimobileos/textures/item/smartphone.png"

    # ディレクトリが存在することを確認
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    # PNG形式で保存
    texture.save(output_path, "PNG")

    print(f"Smartphone texture saved to: {output_path}")
    print("Texture size: 16x16 pixels")

if __name__ == "__main__":
    main()