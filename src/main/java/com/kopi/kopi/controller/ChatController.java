package com.kopi.kopi.controller;

import com.kopi.kopi.dto.chat.ChatRequest;
import com.kopi.kopi.dto.chat.ChatResponse;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.service.IChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apiv1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Integer userId = null;
            String userRole = "GUEST";

            // Cho phép guest chat, nhưng nếu có authentication thì lấy thông tin user
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
                userId = userPrincipal.getUser().getUserId();
                userRole = userPrincipal.getUser().getRole() != null
                        ? userPrincipal.getUser().getRole().getName()
                        : "CUSTOMER";
            }

            ChatResponse response = chatService.processMessage(request, userId, userRole);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                    ChatResponse.builder()
                            .message("Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.")
                            .intent("general")
                            .build()
            );
        }
    }
}

