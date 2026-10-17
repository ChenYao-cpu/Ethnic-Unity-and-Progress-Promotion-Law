package com.backend.com.minzu.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ServerEndpoint("/ws/battle/{userId}")
@Component
public class BattleWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleWebSocketHandler.class);

    // 存储所有在线用户的WebSocket连接
    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    // 存储匹配队列中的用户
    private static final CopyOnWriteArraySet<Long> matchingUsers = new CopyOnWriteArraySet<>();

    // 存储对战房间
    private static final Map<String, BattleRoom> battleRooms = new ConcurrentHashMap<>();

    // 在线用户数
    private static final AtomicInteger onlineCount = new AtomicInteger(0);

    // 定时任务调度器：匹配超时 + 机器人答题延迟
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // 跟踪每个用户的匹配超时任务，用于取消
    private static final Map<Long, ScheduledFuture<?>> matchTimeouts = new ConcurrentHashMap<>();

    // 机器人用户ID（负数，避免与真实用户冲突）
    private static final Long BOT_USER_ID = -1L;

    // 匹配超时秒数
    private static final int MATCH_TIMEOUT_SECONDS = 10;

    // 机器人答题总数
    private static final int BOT_TOTAL_QUESTIONS = 10;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        onlineUsers.put(userId, session);
        onlineCount.incrementAndGet();
        log.info("用户 {} 已连接，当前在线人数: {}", userId, onlineCount.get());

        // 发送连接成功消息
        sendMessage(session, JSON.toJSONString(new Message("CONNECT_SUCCESS", "连接成功")));
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        onlineUsers.remove(userId);
        matchingUsers.remove(userId);
        onlineCount.decrementAndGet();
        cancelMatchTimeout(userId);
        log.info("用户 {} 已断开连接，当前在线人数: {}", userId, onlineCount.get());

        // 如果用户在对战中，通知对手
        notifyOpponent(userId, "OPPONENT_DISCONNECTED", "对手已断开连接");
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") Long userId) {
        log.info("收到用户 {} 的消息: {}", userId, message);

        try {
            JSONObject msg = JSON.parseObject(message);
            String type = msg.getString("type");

            switch (type) {
                case "LOGIN":
                    handleLogin(userId, msg);
                    break;
                case "MATCH_START":
                    handleMatchStart(userId);
                    break;
                case "MATCH_CANCEL":
                    handleMatchCancel(userId);
                    break;
                case "ANSWER_SUBMIT":
                    handleAnswerSubmit(userId, msg);
                    break;
                case "HEARTBEAT":
                    handleHeartbeat(userId);
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendError(userId, "消息处理失败: " + e.getMessage());
        }
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("WebSocket错误，用户: {}", userId, error);
    }

    // 处理登录
    private void handleLogin(Long userId, JSONObject msg) {
        log.info("用户 {} 登录成功", userId);
    }

    // 处理开始匹配
    private void handleMatchStart(Long userId) {
        log.info("用户 {} 开始匹配", userId);

        matchingUsers.add(userId);

        // 发送匹配中状态
        sendMessage(userId, JSON.toJSONString(new Message("MATCH_STATUS", "匹配中...")));

        // 检查是否有其他等待匹配的真实用户
        for (Long otherUserId : matchingUsers) {
            if (!otherUserId.equals(userId)) {
                // 找到真人对手，取消对方的超时任务
                cancelMatchTimeout(otherUserId);
                // 创建对战房间
                createBattleRoom(userId, otherUserId);
                matchingUsers.remove(userId);
                matchingUsers.remove(otherUserId);
                return;
            }
        }

        // 未找到真人对手，安排10秒后匹配机器人
        scheduleBotMatch(userId);
    }

    // 安排机器人匹配超时
    private void scheduleBotMatch(Long userId) {
        ScheduledFuture<?> timeout = scheduler.schedule(() -> {
            if (matchingUsers.contains(userId)) {
                matchingUsers.remove(userId);
                log.info("用户 {} 匹配超时（{}秒），自动匹配AI对手", userId, MATCH_TIMEOUT_SECONDS);
                createBotBattleRoom(userId);
            }
            matchTimeouts.remove(userId);
        }, MATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        matchTimeouts.put(userId, timeout);
        log.info("用户 {} 已安排{}秒后匹配机器人", userId, MATCH_TIMEOUT_SECONDS);
    }

    // 取消匹配超时任务
    private void cancelMatchTimeout(Long userId) {
        ScheduledFuture<?> timeout = matchTimeouts.remove(userId);
        if (timeout != null && !timeout.isDone()) {
            timeout.cancel(false);
            log.info("已取消用户 {} 的匹配超时任务", userId);
        }
    }

    // 处理取消匹配
    private void handleMatchCancel(Long userId) {
        log.info("用户 {} 取消匹配", userId);
        matchingUsers.remove(userId);
        cancelMatchTimeout(userId);
        sendMessage(userId, JSON.toJSONString(new Message("MATCH_CANCELLED", "已取消匹配")));
    }

    // 处理答案提交
    private void handleAnswerSubmit(Long userId, JSONObject msg) {
        Integer questionIndex = msg.getInteger("questionIndex");
        Integer answerIndex = msg.getInteger("answerIndex");

        log.info("用户 {} 提交答案 - 题目{}: {}", userId, questionIndex, answerIndex);

        // 查找用户所在的房间
        BattleRoom room = null;
        Long opponentId = null;

        for (Map.Entry<String, BattleRoom> entry : battleRooms.entrySet()) {
            BattleRoom r = entry.getValue();
            if (r.player1.equals(userId)) {
                room = r;
                opponentId = r.player2;
                break;
            } else if (r.player2.equals(userId)) {
                room = r;
                opponentId = r.player1;
                break;
            }
        }

        if (room == null) {
            log.warn("用户 {} 不在任何对战房间中", userId);
            return;
        }

        // 记录答案
        room.recordAnswer(userId, questionIndex, answerIndex);

        // 通知对手（机器人不需要通知）
        if (opponentId != null && !opponentId.equals(BOT_USER_ID)) {
            JSONObject notification = new JSONObject();
            notification.put("type", "OPPONENT_ANSWER");
            notification.put("questionIndex", questionIndex);
            notification.put("answerIndex", answerIndex);
            notification.put("isCorrect", room.isAnswerCorrect(userId, questionIndex, answerIndex));

            sendMessage(opponentId, notification.toJSONString());
        }

        // 检查是否双方都完成了（防止重复发送）
        if (room.isBattleFinished() && battleRooms.containsKey(room.roomId)) {
            room.markFinished();
            sendBattleResult(room);
        }
    }

    // 处理心跳
    private void handleHeartbeat(Long userId) {
        log.debug("收到用户 {} 的心跳", userId);
    }

    // 创建对战房间（真人 vs 真人）
    private void createBattleRoom(Long player1, Long player2) {
        String roomId = "room_" + System.currentTimeMillis();
        BattleRoom room = new BattleRoom(roomId, player1, player2);
        battleRooms.put(roomId, room);

        log.info("创建对战房间 {}, 玩家: {} vs {}", roomId, player1, player2);

        // 通知玩家1
        JSONObject msg1 = new JSONObject();
        msg1.put("type", "MATCH_SUCCESS");
        msg1.put("battleId", roomId);
        msg1.put("opponent", getPlayerInfo(player2));
        msg1.put("questions", generateQuestions());
        sendMessage(player1, msg1.toJSONString());

        // 通知玩家2
        JSONObject msg2 = new JSONObject();
        msg2.put("type", "MATCH_SUCCESS");
        msg2.put("battleId", roomId);
        msg2.put("opponent", getPlayerInfo(player1));
        msg2.put("questions", generateQuestions());
        sendMessage(player2, msg2.toJSONString());
    }

    // 创建机器人对战房间
    private void createBotBattleRoom(Long userId) {
        String roomId = "bot_" + System.currentTimeMillis();
        BattleRoom room = new BattleRoom(roomId, userId, BOT_USER_ID);
        battleRooms.put(roomId, room);

        log.info("创建AI对战房间 {}, 用户: {} vs AI", roomId, userId);

        JSONObject msg = new JSONObject();
        msg.put("type", "MATCH_SUCCESS");
        msg.put("battleId", roomId);
        msg.put("opponent", getBotPlayerInfo());
        msg.put("questions", generateQuestions());

        sendMessage(userId, msg.toJSONString());

        // 启动机器人答题模拟
        startBotSimulation(roomId, userId);
    }

    // 机器人答题模拟
    private void startBotSimulation(String roomId, Long userId) {
        Random random = new Random();
        int cumulativeDelay = 0;

        for (int i = 0; i < BOT_TOTAL_QUESTIONS; i++) {
            final int questionIndex = i;
            // 每题延迟2-6秒
            cumulativeDelay += 2000 + random.nextInt(4000);
            // 机器人正确率约60%
            final boolean isCorrect = random.nextDouble() < 0.6;
            // 随机选一个答案
            final int answerIndex = random.nextInt(4);

            scheduler.schedule(() -> {
                BattleRoom room = battleRooms.get(roomId);
                if (room == null || room.isFinished()) {
                    return; // 房间已被清理，不再发送
                }

                // 记录机器人答案
                room.recordAnswer(BOT_USER_ID, questionIndex, answerIndex);

                // 发送机器人答题通知给用户
                JSONObject notification = new JSONObject();
                notification.put("type", "OPPONENT_ANSWER");
                notification.put("questionIndex", questionIndex);
                notification.put("answerIndex", answerIndex);
                notification.put("isCorrect", isCorrect);

                sendMessage(userId, notification.toJSONString());

                log.info("AI对手 题目{}: 答案={}, 正确={}", questionIndex, answerIndex, isCorrect);

                // 检查对战是否结束
                if (room.isBattleFinished() && battleRooms.containsKey(roomId)) {
                    room.markFinished();
                    sendBattleResult(room);
                }
            }, cumulativeDelay, TimeUnit.MILLISECONDS);
        }

        log.info("AI对手已安排{}题答题任务，用户: {}", BOT_TOTAL_QUESTIONS, userId);
    }

    // 发送对战结果
    private void sendBattleResult(BattleRoom room) {
        JSONObject result = new JSONObject();
        result.put("type", "BATTLE_FINISHED");
        result.put("player1Score", room.getPlayerScore(room.player1));
        result.put("player2Score", room.getPlayerScore(room.player2));
        result.put("winner", room.getWinner());

        // 只发送给真实用户（机器人不需要结果）
        if (!room.player1.equals(BOT_USER_ID)) {
            sendMessage(room.player1, result.toJSONString());
        }
        if (!room.player2.equals(BOT_USER_ID)) {
            sendMessage(room.player2, result.toJSONString());
        }

        log.info("对战结束 {}, 结果: player1={}, player2={}, winner={}",
                room.roomId, room.getPlayerScore(room.player1),
                room.getPlayerScore(room.player2), room.getWinner());

        // 清理房间
        battleRooms.remove(room.roomId);
    }

    // 通知对手
    private void notifyOpponent(Long userId, String type, String message) {
        for (BattleRoom room : battleRooms.values()) {
            if (room.player1.equals(userId)) {
                if (!room.player2.equals(BOT_USER_ID)) {
                    sendMessage(room.player2, JSON.toJSONString(new Message(type, message)));
                }
            } else if (room.player2.equals(userId)) {
                if (!room.player1.equals(BOT_USER_ID)) {
                    sendMessage(room.player1, JSON.toJSONString(new Message(type, message)));
                }
            }
        }
    }

    // 发送消息给指定用户
    private void sendMessage(Long userId, String message) {
        // 跳过机器人用户
        if (userId.equals(BOT_USER_ID)) {
            return;
        }
        Session session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("发送消息失败，用户: {}", userId, e);
            }
        }
    }

    // 发送消息给Session
    private void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        }
    }

    // 发送错误消息
    private void sendError(Long userId, String errorMsg) {
        JSONObject error = new JSONObject();
        error.put("type", "ERROR");
        error.put("message", errorMsg);
        sendMessage(userId, error.toJSONString());
    }

    // 获取真人玩家信息
    private JSONObject getPlayerInfo(Long userId) {
        JSONObject player = new JSONObject();
        player.put("userId", userId);
        player.put("nickname", "玩家" + userId);
        player.put("avatar", "/images/default_avatar.jpg");
        player.put("level", 1);
        return player;
    }

    // 获取机器人玩家信息（对外显示为普通玩家）
    private JSONObject getBotPlayerInfo() {
        JSONObject bot = new JSONObject();
        bot.put("userId", BOT_USER_ID);
        int botIndex = new Random().nextInt(900) + 100;
        bot.put("nickname", "玩家" + botIndex);
        bot.put("avatar", "/images/default_avatar.jpg");
        bot.put("level", 1);
        return bot;
    }

    // 生成题目（前端自行生成，后端返回空数组）
    private String generateQuestions() {
        return "[]";
    }

    // 消息类
    private static class Message {
        private String type;
        private String message;

        public Message(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
    }

    // 对战房间类
    private static class BattleRoom {
        String roomId;
        Long player1;
        Long player2;
        Map<Long, Map<Integer, Integer>> playerAnswers = new ConcurrentHashMap<>();
        Map<Long, Integer> playerScores = new ConcurrentHashMap<>();
        private volatile boolean finished = false;

        public BattleRoom(String roomId, Long player1, Long player2) {
            this.roomId = roomId;
            this.player1 = player1;
            this.player2 = player2;
            playerAnswers.put(player1, new ConcurrentHashMap<>());
            playerAnswers.put(player2, new ConcurrentHashMap<>());
            playerScores.put(player1, 0);
            playerScores.put(player2, 0);
        }

        public void recordAnswer(Long userId, Integer questionIndex, Integer answerIndex) {
            playerAnswers.get(userId).put(questionIndex, answerIndex);
        }

        public boolean isAnswerCorrect(Long userId, Integer questionIndex, Integer answerIndex) {
            // 简化为随机判断正确性（前端自行判断）
            // 机器人答案的正确性由startBotSimulation中的isCorrect决定
            return Math.random() > 0.5;
        }

        public int getPlayerScore(Long userId) {
            return playerScores.getOrDefault(userId, 0);
        }

        public boolean isBattleFinished() {
            // 双方各答满10题
            return playerAnswers.get(player1).size() >= 10
                    && playerAnswers.get(player2).size() >= 10;
        }

        public void markFinished() {
            this.finished = true;
        }

        public boolean isFinished() {
            return finished;
        }

        public Long getWinner() {
            int score1 = playerScores.getOrDefault(player1, 0);
            int score2 = playerScores.getOrDefault(player2, 0);
            if (score1 > score2) return player1;
            if (score2 > score1) return player2;
            return null; // 平局
        }
    }
}
