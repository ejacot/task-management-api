package com.ejacot.taskmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskManagementApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void userCanRegisterAndManageOwnTasks() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"portfolio-user","password":"strong-password"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("portfolio-user"));

        String taskJson = mvc.perform(post("/api/tasks")
                        .with(httpBasic("portfolio-user", "strong-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ship portfolio API","description":"Add tests and documentation","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(taskJson).get("id").asLong();
        mvc.perform(get("/api/tasks/{id}", id).with(httpBasic("portfolio-user", "strong-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ship portfolio API"));

        mvc.perform(delete("/api/tasks/{id}", id).with(httpBasic("portfolio-user", "strong-password")))
                .andExpect(status().isNoContent());
    }

    @Test
    void anonymousUsersCannotReadTasks() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }
}

