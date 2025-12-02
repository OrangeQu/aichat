package com.orangequ.aichat.service.impl;

import com.orangequ.aichat.mode.ChatRoom;
import com.orangequ.aichat.service.Aimanage;
import com.orangequ.aichat.service.ChatService;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    @Resource
    private Aimanage aiManager;

    /**
     * 引导模型只出题并按“是/否/与此无关”作答，避免自问自答或一次性输出完整对话。
     */
    private static final String SYSTEM_PROMPT = "你是一位脑筋急转弯“海龟汤”游戏主持人。当用户说“开始”时，先给出一道题目，并提醒只能回答“是”“否”“与此无关”。之后每次回复只给出这三类回答或简短引导，绝不要自己连续提问并回答，也不要一次把整个对话或答案全部说完。用户结束或你判断可以结束时，回复“游戏结束”并给出汤底。";

    Map<Long, List<ChatMessage>> chatHistories = new HashMap<>();

    @Override
    public String doChat(Long roomId, String userPrompt) {
        List<ChatMessage> messages = chatHistories.computeIfAbsent(roomId, key -> {
            List<ChatMessage> init = new ArrayList<>();
            init.add(ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(SYSTEM_PROMPT).build());
            return init;
        });

        final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(userPrompt).build();
        messages.add(userMessage);

        String answer = aiManager.doChat(messages);
        final ChatMessage answerMessage = ChatMessage.builder().role(ChatMessageRole.ASSISTANT).content(answer).build();
        messages.add(answerMessage);

        if (answer.contains("游戏结束")) {
            chatHistories.remove(roomId);
        }

        // 返回模型回答
        return answer;
    }

    @Override
    public List<ChatRoom> getChatRooms() {
        List<ChatRoom> chatRoomList = new ArrayList<>();
        for (Map.Entry<Long, List<ChatMessage>> entry : chatHistories.entrySet()) {
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setRoomId(entry.getKey());
            List<ChatMessage> messages = entry.getValue();
            // 返回历史时过滤掉系统提示，避免在前端显示
            List<ChatMessage> visibleMessages = new ArrayList<>();
            for (ChatMessage message : messages) {
                if (ChatMessageRole.SYSTEM.equals(message.getRole())) {
                    continue;
                }
                visibleMessages.add(message);
            }
            chatRoom.setChatMessage(visibleMessages);
            chatRoomList.add(chatRoom);
        }
        return chatRoomList;
    }
}
