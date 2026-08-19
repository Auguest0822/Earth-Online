package com.earthonline.backend.chat;

import com.earthonline.backend.chat.dto.ChatMessageResponse;
import com.earthonline.backend.chat.dto.ChatReadReceiptResponse;
import com.earthonline.backend.chat.dto.ChatUnreadCountResponse;
import com.earthonline.backend.chat.dto.SendChatMessageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /*
    @GetMapping("/messages")
    public List<ChatMessageResponse> getMessages(
            @RequestParam
            @Pattern(regexp = "\\d{10}", message = "好友地球 ID 必须为 10 位数字")
            String friendPublicId,
            Authentication authentication
    ) {
        return chatService.getConversation(authentication.getName(), friendPublicId);
    }
    */

    @GetMapping("/messages")
    public List<ChatMessageResponse> getMessages(
            @RequestParam @Pattern(regexp = "\\d{10}") String friendPublicId,
            Authentication authentication
    ) {
        return chatService.getConversation(authentication.getName(), friendPublicId);
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(
            @Valid @RequestBody SendChatMessageRequest request,
            Authentication authentication
    ) {
        return chatService.sendMessage(authentication.getName(), request);
    }

    @GetMapping("/unread-counts")
    public List<ChatUnreadCountResponse> getUnreadCounts(Authentication authentication) {
        return chatService.getUnreadCounts(authentication.getName());
    }

    @PostMapping("/messages/read")
    public ChatReadReceiptResponse markMessagesRead(
            @RequestParam @Pattern(regexp = "\\d{10}") String friendPublicId,
            Authentication authentication
    ) {
        return chatService.markConversationRead(authentication.getName(), friendPublicId);
    }
}
