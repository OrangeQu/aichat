package com.orangequ.aichat.service;

import com.orangequ.aichat.mode.ChatRoom;

import java.util.List;

public interface ChatService {

    String doChat(Long roomId,String userPrompt);

    List<ChatRoom> getChatRooms();
}
