package com.ejacot.taskmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class HotelWorkApiIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void employeeCanLoadHotelDashboardAndRegisterRoomWork() throws Exception {
        String body = mvc.perform(get("/api/hotel/bootstrap").with(httpBasic("mariana", "demo1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotel.name").value("Infinity Hotel"))
                .andExpect(jsonPath("$.me.hourlyRate").value(17.25))
                .andReturn().getResponse().getContentAsString();

        long roomTypeId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body)
                .get("workTypes").get(0).get("id").asLong();

        mvc.perform(post("/api/hotel/logs")
                        .with(httpBasic("mariana", "demo1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workTypeId":%d,"workDate":"2026-07-06","quantity":12,"roomType":"NORMAL"}
                                """.formatted(roomTypeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hours").value(5.00))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}
