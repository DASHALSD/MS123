package com.itm.space.backendresources.controller;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UUID userId;
    private UserRequest testUserRequest;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUserRequest = new UserRequest("testUser", "test@domain.com", "password", "John", "Doe");
        testUserResponse = new UserResponse("testUser", "Doe", "test@domain.com", List.of("ROLE_USER"), List.of("GROUP_1"));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Тест создания пользователя")
    void testCreateUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "testUser",
                                    "email": "test@domain.com",
                                    "password": "password",
                                    "firstName": "John",
                                    "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isOk());

        verify(userService, times(1)).createUser(any(UserRequest.class));
    }

    @Test
    @WithMockUser(username = "moderatorUser", roles = "MODERATOR")
    @DisplayName("Тест получения приветствия")
    void testHello() throws Exception {
        mockMvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("moderatorUser"));
    }


    @Nested
    @DisplayName("Тесты для получения пользователя по ID")
    class GetUserByIdTests {

        @Test
        @WithMockUser(roles = "MODERATOR")
        @DisplayName("Пользователь существует - успешный запрос")
        void shouldReturnUserResponseWhenUserExists() throws Exception {
            UUID userId = UUID.randomUUID();

            UserResponse userResponse = new UserResponse(
                    "FirstName",
                    "LastName",
                    "email_test@example.com",
                    List.of("ROLE_USER"),
                    List.of("GROUP_1")
            );

            when(userService.getUserById(userId)).thenReturn(userResponse);

            mockMvc.perform(get("/api/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("email_test@example.com"));
        }

        @Test
        @WithMockUser(roles = "MODERATOR")
        @DisplayName("Пользователь не найден - ошибка 404")
        void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
            UUID userId = UUID.randomUUID();

            when(userService.getUserById(userId))
                    .thenThrow(new BackendResourcesException("User not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/users/{id}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}
