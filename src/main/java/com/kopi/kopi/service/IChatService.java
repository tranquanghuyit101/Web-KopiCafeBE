package com.kopi.kopi.service;

import com.kopi.kopi.dto.chat.ChatRequest;
import com.kopi.kopi.dto.chat.ChatResponse;

public interface IChatService {
    ChatResponse processMessage(ChatRequest request, Integer userId, String userRole);
}

