package com.outreach.auth.api;

import com.outreach.auth.application.AuthService;
import com.outreach.auth.domain.AuthTokenResponse;
import com.outreach.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    void testRegister() throws Exception {
        User u = new User("test@test.com", "password", "First", "Last");
        u.setId(1L);
        when(authService.register(anyString(), anyString(), anyString(), anyString())).thenReturn(u);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"password\":\"password\", \"firstName\":\"First\", \"lastName\":\"Last\"}"))
                .andExpect(status().isOk());
        
        verify(authService, times(1)).register(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testLogin() throws Exception {
        AuthTokenResponse r = new AuthTokenResponse("test-token", "Bearer", 1L, "test@test.com", "First", "Last", 1L);
        when(authService.login(anyString(), anyString())).thenReturn(r);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-token"));
        
        verify(authService, times(1)).login(anyString(), anyString());
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testVerify() throws Exception {
        doNothing().when(authService).verify(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"code\":\"123456\"}"))
                .andExpect(status().isOk());
        
        verify(authService, times(1)).verify(anyString(), anyString());
    }

    @Test
    void testRegisterWithInvalidEmail() throws Exception {
        when(authService.register(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existing@test.com\", \"password\":\"password\", \"firstName\":\"First\", \"lastName\":\"Last\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testVerifyWithInvalidCode() throws Exception {
        doThrow(new IllegalArgumentException("Invalid verification code"))
                .when(authService).verify(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"code\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidateToken() throws Exception {
        when(authService.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void testValidateTokenWithoutBearer() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

}
