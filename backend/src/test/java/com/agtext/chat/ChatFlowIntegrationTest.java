package com.agtext.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.agtext.TestModelConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestModelConfig.class)
class ChatFlowIntegrationTest {
  @Autowired private TestRestTemplate rest;

  @Test
  void shouldCreateConversationAndPersistMessages() {
    ChatResponse r1 = postChat(new ChatRequest(null, "你好", null, null));
    assertThat(r1.conversationId()).startsWith("cnv_");
    assertThat(r1.assistantMessage()).isEqualTo("mock:你好");

    ChatResponse r2 = postChat(new ChatRequest(r1.conversationId(), "再来一次", null, null));
    assertThat(r2.conversationId()).isEqualTo(r1.conversationId());
    assertThat(r2.assistantMessage()).isEqualTo("mock:再来一次");

    MessageItem[] msgs =
        rest.getForObject(
            "/api/conversations/" + r1.conversationId() + "/messages", MessageItem[].class);
    assertThat(msgs).isNotNull();
    assertThat(List.of(msgs)).hasSize(4);
    assertThat(msgs[0].role()).isEqualTo("user");
    assertThat(msgs[0].content()).isEqualTo("你好");
    assertThat(msgs[1].role()).isEqualTo("assistant");
    assertThat(msgs[2].role()).isEqualTo("user");
    assertThat(msgs[3].role()).isEqualTo("assistant");

    UnreadCountDto unread =
        rest.getForObject("/api/notifications/unread-count", UnreadCountDto.class);
    assertThat(unread).isNotNull();
    assertThat(unread.count()).isGreaterThanOrEqualTo(1);
  }

  private ChatResponse postChat(ChatRequest req) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.postForObject("/api/chat", new HttpEntity<>(req, headers), ChatResponse.class);
  }

  private record ChatRequest(
      String conversationId, String message, String provider, String model) {}

  private record ChatResponse(
      String conversationId,
      String provider,
      String model,
      String assistantMessage,
      String createdAt) {}

  private record MessageItem(String id, String conversationId, String role, String content) {}

  private record UnreadCountDto(long count) {}
}
