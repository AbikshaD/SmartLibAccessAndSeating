package library_management.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRecommendationRequest(
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 10) List<@Valid ConversationMessage> conversation) {

    public record ConversationMessage(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 1000) String content) { }
}
