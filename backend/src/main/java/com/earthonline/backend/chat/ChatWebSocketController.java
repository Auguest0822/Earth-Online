package com.earthonline.backend.chat;

import com.earthonline.backend.chat.dto.SendChatMessageRequest;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Receives STOMP chat commands from /app/chat/messages.
 *
 * The current user comes from the JWT-bound WebSocket Principal, never from
 * the browser payload. A new message is stored first, then pushed to the
 * recipient's /user/queue/chat/messages subscription.
 */
@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat/messages")
    public void sendMessage(
            @Valid @Payload SendChatMessageRequest request,
            Principal principal
    ) {
        chatService.sendMessageAndPush(principal.getName(), request);
    }
}
