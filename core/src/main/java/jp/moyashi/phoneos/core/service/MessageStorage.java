package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * メッセージストレージサービス
 * プレイヤー間のメッセージを保存・取得します
 */
public class MessageStorage {

    private final VFS vfs;

    /**
     * メッセージデータクラス
     */
    public static class Message {
        private final String id;
        private final String senderAddress;
        private final String senderName;
        private final String content;
        private final long timestamp;
        private boolean read;

        public Message(String id, String senderAddress, String senderName, String content, long timestamp, boolean read) {
            this.id = id;
            this.senderAddress = senderAddress;
            this.senderName = senderName;
            this.content = content;
            this.timestamp = timestamp;
            this.read = read;
        }

        public String getId() {
            return id;
        }

        public String getSenderAddress() {
            return senderAddress;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }

        /**
         * CSV形式の文字列に変換
         */
        public String toCsvLine() {
            return id + "," + senderAddress + "," + escapeCsv(senderName) + "," +
                   escapeCsv(content) + "," + timestamp + "," + read;
        }

        /**
         * CSV行からメッセージを復元
         */
        public static Message fromCsvLine(String line) {
            String[] parts = parseCsvLine(line);
            if (parts.length >= 6) {
                return new Message(
                    parts[0],
                    parts[1],
                    unescapeCsv(parts[2]),
                    unescapeCsv(parts[3]),
                    Long.parseLong(parts[4]),
                    Boolean.parseBoolean(parts[5])
                );
            }
            return null;
        }

        private static String escapeCsv(String text) {
            if (text == null) return "";
            return "\"" + text.replace("\"", "\"\"").replace("\n", "\\n") + "\"";
        }

        private static String unescapeCsv(String text) {
            if (text == null) return "";
            if (text.startsWith("\"") && text.endsWith("\"")) {
                text = text.substring(1, text.length() - 1);
            }
            return text.replace("\"\"", "\"").replace("\\n", "\n");
        }

        private static String[] parseCsvLine(String line) {
            List<String> result = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '"') {
                    if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = !inQuotes;
                    }
                } else if (c == ',' && !inQuotes) {
                    result.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            result.add(current.toString());

            return result.toArray(new String[0]);
        }
    }

    /**
     * MessageStorageを構築します
     * @param vfs VFSインスタンス
     */
    public MessageStorage(VFS vfs) {
        this.vfs = vfs;
    }

    /**
     * メッセージを保存します
     * @param message メッセージ
     */
    public void saveMessage(Message message) {
        String messagesFile = vfs.getRootPath().resolve("messages/inbox.csv").toString();

        try {
            // ファイルが存在しない場合は作成
            java.io.File file = new java.io.File(messagesFile);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            // 追記モードで書き込み
            try (FileWriter writer = new FileWriter(messagesFile, true)) {
                writer.write(message.toCsvLine() + "\n");
            }

            System.out.println("[MessageStorage] Message saved: " + message.getId());

        } catch (IOException e) {
            System.err.println("[MessageStorage] Failed to save message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * すべてのメッセージを取得します
     * @return メッセージリスト（新しい順）
     */
    public List<Message> getAllMessages() {
        String messagesFile = vfs.getRootPath().resolve("messages/inbox.csv").toString();
        List<Message> messages = new ArrayList<>();

        try {
            if (!vfs.fileExists("messages/inbox.csv")) {
                return messages;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(messagesFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    Message message = Message.fromCsvLine(line);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }

            // 新しい順に並び替え
            Collections.reverse(messages);

        } catch (IOException e) {
            System.err.println("[MessageStorage] Failed to load messages: " + e.getMessage());
        }

        return messages;
    }

    /**
     * 未読メッセージ数を取得します
     * @return 未読メッセージ数
     */
    public int getUnreadCount() {
        List<Message> messages = getAllMessages();
        int count = 0;
        for (Message message : messages) {
            if (!message.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * メッセージを既読にします
     * @param messageId メッセージID
     */
    public void markAsRead(String messageId) {
        List<Message> messages = getAllMessages();
        boolean updated = false;

        for (Message message : messages) {
            if (message.getId().equals(messageId)) {
                message.setRead(true);
                updated = true;
            }
        }

        if (updated) {
            saveAllMessages(messages);
        }
    }

    /**
     * すべてのメッセージを保存し直します
     * @param messages メッセージリスト
     */
    private void saveAllMessages(List<Message> messages) {
        String messagesFile = vfs.getRootPath().resolve("messages/inbox.csv").toString();

        try {
            // 逆順に戻す（ファイルは古い順）
            Collections.reverse(messages);

            try (FileWriter writer = new FileWriter(messagesFile, false)) {
                for (Message message : messages) {
                    writer.write(message.toCsvLine() + "\n");
                }
            }

        } catch (IOException e) {
            System.err.println("[MessageStorage] Failed to save all messages: " + e.getMessage());
        }
    }

    /**
     * 新しいメッセージを受信したときに呼び出されます
     * @param senderAddress 送信元アドレス
     * @param senderName 送信者名
     * @param content メッセージ内容
     */
    public void receiveMessage(String senderAddress, String senderName, String content) {
        String messageId = "msg_" + System.currentTimeMillis();
        Message message = new Message(
            messageId,
            senderAddress,
            senderName,
            content,
            System.currentTimeMillis(),
            false
        );
        saveMessage(message);
    }
}