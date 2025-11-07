# MochiMobileOS 開発ドキュメント

このドキュメントはMochiMobileOSの技術的な概要と各機能へのポータルです。

## 開発ドキュメント一覧

より詳細な技術仕様や実装については、以下の各ドキュメントを参照してください。

- **[00_Project_Overview.md](./docs/development/00_Project_Overview.md)**
  - プロジェクト全体の概要、アーキテクチャ、主要機能について説明します。

- **[01_Chromium_Integration.md](./docs/development/01_Chromium_Integration.md)**
  - Chromium (JCEF/MCEF) の統合アーキテクチャ、パフォーマンス最適化の経緯、UI設計について詳述します。

- **[02_WebView_System.md](./docs/development/02_WebView_System.md)**
  - JavaFX WebViewベースのWebレンダリングシステムに関する情報です。

- **[03_Virtual_Internet.md](./docs/development/03_Virtual_Internet.md)**
  - MOD内仮想ネットワーク（IPvM）の仕様と実装について説明します。

- **[04_Forge_Integration.md](./docs/development/04_Forge_Integration.md)**
  - Minecraft Forge MODとしての連携部分のアーキテクチャと実装について説明します。

- **[05_Hardware_API.md](./docs/development/05_Hardware_API.md)**
  - 仮想ハードウェア（センサー、通信、バッテリー等）の抽象化APIに関する仕様です。

- **[06_External_App_API.md](./docs/development/06_External_App_API.md)**
  - 外部MOD開発者向けのアプリケーション開発API（パーミッション、インテント等）について説明します。

- **[07_GUI_Component_Library.md](./docs/development/07_GUI_Component_Library.md)**
  - 再利用可能なUIコンポーネントライブラリの設計と使用方法について説明します。

- **[08_Font_System.md](./docs/development/08_Font_System.md)**
  - 多言語（特に日本語）フォントの表示を実現するためのフォントシステムについて説明します。

- **[09_Known_Issues_And_TODO.md](./docs/development/09_Known_Issues_And_TODO.md)**
  - 現在把握している既知の問題点と、今後の開発タスク（TODO）を管理します。

## 開発ログ

日々の詳細な開発ログは `devlogs` ディレクトリに格納されています。

- [devlogs/2025-10-14.md](./devlogs/2025-10-14.md)
- [devlogs/2025-10-13.md](./devlogs/2025-10-13.md)