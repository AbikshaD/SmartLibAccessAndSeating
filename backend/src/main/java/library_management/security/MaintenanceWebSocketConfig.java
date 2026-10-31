package library_management.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class MaintenanceWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final MaintenanceWebSocketAuthenticationInterceptor authenticationInterceptor;
    @Value("${app.frontend-origin:}")
    private String frontendOrigin;

    public MaintenanceWebSocketConfig(MaintenanceWebSocketAuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:*", "http://127.0.0.1:*",
                "https://bullring-language-corset.ngrok-free.dev",
                "https://*.ngrok-free.dev", "https://*.ngrok-free.app"));
        allowedOrigins.addAll(SecurityConfig.parseFrontendOrigins(frontendOrigin));
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authenticationInterceptor);
    }
}
