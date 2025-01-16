package com.itm.space.backendresources.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class UserServiceImplIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private UserServiceImpl userService;

    private String createdUserId;
    private UserRequest newUserRequest;

    @BeforeEach
    void setUp() {

        List<UserRepresentation> existingUsers = keycloak.realm("ITM").users().search("testUser");
        if (!existingUsers.isEmpty()) {
            for (UserRepresentation user : existingUsers) {
                try {
                    keycloak.realm("ITM").users().get(user.getId()).remove();
                    log.info("Удален существующий пользователь с ID: {}", user.getId());
                } catch (Exception e) {
                    log.warn("Не удалось удалить существующего пользователя с ID: {}", user.getId());
                }
            }
        }

        newUserRequest = new UserRequest(
                "testUser",
                "test@example.com",
                "securePassword",
                "Test",
                "User"
        );
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Создание нового пользователя")
    void createUser() throws Exception {
        mvc.perform(post("/api/users")
                        .content(new ObjectMapper().writeValueAsString(newUserRequest))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());


        List<UserRepresentation> users = keycloak.realm("ITM").users().search("testUser");
        assertFalse(users.isEmpty(), "Пользователь не был создан");
        createdUserId = users.get(0).getId();
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Получение существующего пользователя по ID")
    void shouldReturnExistingUserById() throws Exception {
        // Создание пользователя через сервис
        userService.createUser(newUserRequest);

        // Поиск пользователя в Keycloak
        List<UserRepresentation> users = keycloak.realm("ITM").users().search("testUser");
        assertFalse(users.isEmpty(), "Пользователь не найден");
        createdUserId = users.get(0).getId();


        mvc.perform(get("/api/users/{id}", createdUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Ошибка при запросе несуществующего пользователя")
    void shouldReturnErrorForNonExistingUser() throws Exception {
        UUID invalidUserId = UUID.randomUUID();

        mvc.perform(get("/api/users/{id}", invalidUserId))
                .andExpect(status().isInternalServerError());
    }

    @AfterEach
    @DisplayName("Очистка созданных данных")
    void cleanup() {
        if (createdUserId != null) {
            try {
                keycloak.realm("ITM").users().get(createdUserId).remove();
                log.info("Удален пользователь с ID: {}", createdUserId);
            } catch (Exception e) {
                log.warn("Не удалось удалить пользователя с ID: {}", createdUserId);
            }

            List<UserRepresentation> users = keycloak.realm("ITM").users().search("testUser");
            assertTrue(users.isEmpty(), "Пользователь не был удалён из Keycloak");

            createdUserId = null;
        }
    }
}

