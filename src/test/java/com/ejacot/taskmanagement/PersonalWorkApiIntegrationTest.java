package com.ejacot.taskmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalWorkApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void userCanRegisterActivateConfigureProfileAndTrackWork() throws Exception {
        String register = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user.one@example.com","password":"strongpass1","confirmPassword":"strongpass1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user.one@example.com"))
                .andReturn().getResponse().getContentAsString();

        String code = objectMapper.readTree(register).get("demoCode").asText();

        mvc.perform(post("/api/auth/register/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
                .andExpect(status().isOk());

        String login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"user.one@example.com","password":"strongpass1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingComplete").value(false))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(login).get("token").asText();

        mvc.perform(put("/api/me/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Pop","defaultHourlyRate":17.25,"currency":"EUR","defaultBreakMinutes":30,"language":"ro"}
                                """))
                .andExpect(status().isNoContent());

        String bootstrap = mvc.perform(get("/api/app/bootstrap").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.me.firstName").value("Ana"))
                .andExpect(jsonPath("$.workTypes.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andReturn().getResponse().getContentAsString();

        JsonNode workTypes = objectMapper.readTree(bootstrap).get("workTypes");
        long workTypeId = workTypes.get(0).get("id").asLong();

        String entry = mvc.perform(post("/api/work-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-07-10","workTypeId":%d,"startTime":"09:00","endTime":"17:30","breakMinutes":30,"notes":"Test shift"}
                                """.formatted(workTypeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hoursWorked").value(8.0))
                .andExpect(jsonPath("$.grossAmount").value(138.0))
                .andReturn().getResponse().getContentAsString();

        long entryId = objectMapper.readTree(entry).get("id").asLong();

        mvc.perform(get("/api/work-entries/today?date=2026-07-10").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].id").value(entryId))
                .andExpect(jsonPath("$.totalHours").value(8.0));

        mvc.perform(get("/api/work-entries/calendar?year=2026&month=7").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[9].entries[0].id").value(entryId));

        mvc.perform(get("/api/statistics/summary?year=2026&month=7").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHoursWorked").value(8.0))
                .andExpect(jsonPath("$.totalDaysWorked").value(1));

        mvc.perform(get("/api/export/csv?year=2026&month=7").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("roomly-export.csv")));
    }

    @Test
    void usersCanAccessOnlyTheirOwnEntries() throws Exception {
        String tokenA = createAndActivate("isolated.a@example.com");
        String tokenB = createAndActivate("isolated.b@example.com");

        mvc.perform(put("/api/me/profile").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"User","lastName":"A","defaultHourlyRate":16.0,"currency":"EUR","defaultBreakMinutes":30,"language":"ro"}
                                """))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/me/profile").header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"User","lastName":"B","defaultHourlyRate":18.0,"currency":"EUR","defaultBreakMinutes":30,"language":"ro"}
                                """))
                .andExpect(status().isNoContent());

        long workTypeA = firstWorkType(tokenA);
        String entryA = mvc.perform(post("/api/work-entries")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-07-09","workTypeId":%d,"startTime":"08:00","endTime":"16:30","breakMinutes":30}
                                """.formatted(workTypeA)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long entryId = objectMapper.readTree(entryA).get("id").asLong();

        mvc.perform(get("/api/work-entries?from=2026-07-09&to=2026-07-09").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(0));

        mvc.perform(delete("/api/work-entries/{id}", entryId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void webApplicationIsPubliclyAvailable() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("roomly work")));
    }

    private String createAndActivate(String email) throws Exception {
        String register = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"strongpass1","confirmPassword":"strongpass1"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(register).get("demoCode").asText();
        mvc.perform(post("/api/auth/register/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
                .andExpect(status().isOk());
        String login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"strongpass1"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(login).get("token").asText();
    }

    private long firstWorkType(String token) throws Exception {
        String bootstrap = mvc.perform(get("/api/app/bootstrap").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(bootstrap).get("workTypes").get(0).get("id").asLong();
    }
}
