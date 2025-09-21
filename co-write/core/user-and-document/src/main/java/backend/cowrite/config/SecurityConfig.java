package backend.cowrite.config;

import backend.cowrite.auth.CustomOAuth2UserDetailService;
import backend.cowrite.auth.filter.LoginFilter;
import backend.cowrite.auth.filter.TokenAuthenticationFilter;
import backend.cowrite.auth.filter.handler.LoginSuccessHandler;
import backend.cowrite.auth.filter.handler.OAuth2AuthenticationFailureHandler;
import backend.cowrite.auth.filter.handler.OAuth2AuthenticationSuccessHandler;
import backend.cowrite.auth.filter.util.HttpCookieOAuth2Auth2AuthorizationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.filters.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthenticationFilter tokenAuthenticationFilter;
    private final LoginSuccessHandler loginSuccessHandler;
    private final CustomOAuth2UserDetailService customOAuth2UserDetailService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public HttpCookieOAuth2Auth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2Auth2AuthorizationRequestRepository();
    }

    @Bean
    public LoginFilter loginFilter(AuthenticationManager authenticationManager) {
        LoginFilter loginFilter = new LoginFilter();
        loginFilter.setFilterProcessesUrl("/api/auth/login");
        loginFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        loginFilter.setAuthenticationManager(authenticationManager);
        return loginFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(aep -> aep
                                .baseUri("/api/oauth2/authorize")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository()))
                        .redirectionEndpoint(rep -> rep
                                .baseUri("/api/oauth2/callback/*"))
                        .userInfoEndpoint(uep -> uep.userService(customOAuth2UserDetailService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );

        http.addFilterAfter(tokenAuthenticationFilter, CorsFilter.class);
        http.addFilterAfter(loginFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
