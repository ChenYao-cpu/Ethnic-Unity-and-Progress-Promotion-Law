package com.backend.com.minzu.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.backend.com.minzu.entity.BattleRecord;
import com.backend.com.minzu.service.BattleRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/battle")
@Component
public class BattleWebSocket {

    private static BattleRecordService battleRecordService;

    @Autowired
    public void setBattleRecordService(BattleRecordService battleRecordService) {
        BattleWebSocket.battleRecordService = battleRecordService;
    }

    private static final Map<Long, Session> ONLINE_USERS = new ConcurrentHashMap<>();
    private static final Map<String, BattleRoom> BATTLE_ROOMS = new ConcurrentHashMap<>();

    static class BattleRoom {
        Long player1Id;
        Long player2Id;
        Session player1Session;
        Session player2Session;
        BattleRecord record;

        BattleRoom(Long p1, Long p2, Session s1, Session s2, BattleRecord record) {
            this.player1Id = p1;
            this.player2Id = p2;
            this.player1Session = s1;
            this.player2Session = s2;
            this.record = record;
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        String queryString = session.getQueryString();
        if (queryString != null && queryString.contains("userId=")) {
            Long userId = Long.parseLong(queryString.split("=")[1]);
            ONLINE_USERS.put(userId, session);
            System.out.println("用户" + userId + "上线");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject json = JSON.parseObject(message);
            String action = json.getString("action");
            Long userId = json.getLong("userId");

            switch (action) {
                case "startMatching":
                    handleStartMatching(userId, session);
                    break;
                case "submitAnswer":
                    handleSubmitAnswer(userId, json);
                    break;
                case "cancelMatching":
                    handleCancelMatching(userId);
                    break;
                case "battleFinished":
                    handleBattleFinished(userId, json);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStartMatching(Long userId, Session session) {
        if (ONLINE_USERS.size() < 2) {
            Map<String, Object> data = new HashMap<>();
            data.put("action", "waiting");
            sendMessage(userId, JSON.toJSONString(data));
            return;
        }

        Long opponentId = ONLINE_USERS.keySet().stream()
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);

        if (opponentId != null) {
            String battleId = "battle_" + System.currentTimeMillis();
            Session opponentSession = ONLINE_USERS.get(opponentId);

            BattleRecord record = new BattleRecord();
            record.setBattleId(battleId);
            record.setPlayerId1(userId);
            record.setPlayerId2(opponentId);
            record.setStatus(1);
            record.setStartTime(LocalDateTime.now());
            battleRecordService.save(record);

            BattleRoom room = new BattleRoom(userId, opponentId, session, opponentSession, record);
            BATTLE_ROOMS.put(battleId, room);

            Map<String, Object> data1 = new HashMap<>();
            data1.put("action", "matchSuccess");
            data1.put("battleId", battleId);
            data1.put("opponentId", opponentId);
            data1.put("isPlayer1", true);
            sendMessage(userId, JSON.toJSONString(data1));

            Map<String, Object> data2 = new HashMap<>();
            data2.put("action", "matchSuccess");
            data2.put("battleId", battleId);
            data2.put("opponentId", userId);
            data2.put("isPlayer1", false);
            sendMessage(opponentId, JSON.toJSONString(data2));
        }
    }

    private void handleSubmitAnswer(Long userId, JSONObject json) {
        String battleId = json.getString("battleId");
        Integer questionIndex = json.getInteger("questionIndex");
        Integer answerIndex = json.getInteger("answerIndex");
        Boolean isCorrect = json.getBoolean("isCorrect");

        BattleRoom room = BATTLE_ROOMS.get(battleId);
        if (room != null) {
            Long opponentId = room.player1Id.equals(userId) ? room.player2Id : room.player1Id;

            Map<String, Object> data = new HashMap<>();
            data.put("action", "opponentAnswer");
            data.put("userId", userId);
            data.put("questionIndex", questionIndex);
            data.put("answerIndex", answerIndex);
            data.put("isCorrect", isCorrect);
            sendMessage(opponentId, JSON.toJSONString(data));
        }
    }

    private void handleBattleFinished(Long userId, JSONObject json) {
        String battleId = json.getString("battleId");
        Integer score = json.getInteger("score");
        Integer correctCount = json.getInteger("correctCount");

        BattleRoom room = BATTLE_ROOMS.get(battleId);
        if (room != null) {
            if (room.player1Id.equals(userId)) {
                room.record.setPlayer1Score(score);
                room.record.setPlayer1CorrectCount(correctCount);
            } else {
                room.record.setPlayer2Score(score);
                room.record.setPlayer2CorrectCount(correctCount);
            }

            if (room.record.getPlayer1Score() != null && room.record.getPlayer2Score() != null) {
                if (room.record.getPlayer1Score() > room.record.getPlayer2Score()) {
                    room.record.setWinnerId(room.player1Id);
                } else if (room.record.getPlayer2Score() > room.record.getPlayer1Score()) {
                    room.record.setWinnerId(room.player2Id);
                }
                room.record.setStatus(2);
                room.record.setEndTime(LocalDateTime.now());
                battleRecordService.updateById(room.record);

                Map<String, Object> data = new HashMap<>();
                data.put("action", "battleResult");
                data.put("player1Score", room.record.getPlayer1Score());
                data.put("player2Score", room.record.getPlayer2Score());
                data.put("player1Correct", room.record.getPlayer1CorrectCount());
                data.put("player2Correct", room.record.getPlayer2CorrectCount());
                data.put("winnerId", room.record.getWinnerId());
                sendMessage(room.player1Id, JSON.toJSONString(data));
                sendMessage(room.player2Id, JSON.toJSONString(data));

                BATTLE_ROOMS.remove(battleId);
            }
        }
    }

    private void handleCancelMatching(Long userId) {
        ONLINE_USERS.remove(userId);
    }

    private void sendMessage(Long userId, String message) {
        Session session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        String queryString = session.getQueryString();
        if (queryString != null && queryString.contains("userId=")) {
            Long userId = Long.parseLong(queryString.split("=")[1]);
            ONLINE_USERS.remove(userId);
            System.out.println("用户" + userId + "下线");
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }
}
