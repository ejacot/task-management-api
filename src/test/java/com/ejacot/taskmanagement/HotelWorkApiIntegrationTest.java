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

        var workTypes = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("workTypes");
        long roomTypeId = java.util.stream.StreamSupport.stream(workTypes.spliterator(), false)
                .filter(type -> "ROOMS".equals(type.get("unit").asText()))
                .findFirst().orElseThrow().get("id").asLong();

        String logBody = mvc.perform(post("/api/hotel/logs")
                        .with(httpBasic("mariana", "demo1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workTypeId":%d,"workDate":"2026-07-06","normalRooms":12,"juniorRooms":0,"presidentRooms":0,"roomType":"NORMAL"}
                                """.formatted(roomTypeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hours").value(5.00))
                .andExpect(jsonPath("$.normalRooms").value(12))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        long logId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(logBody).get("id").asLong();
        mvc.perform(put("/api/hotel/logs/{id}/submit",logId).with(httpBasic("mariana","demo1234")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(put("/api/management/logs/{id}/review",logId).with(httpBasic("checker","checker1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void managerCanPublishAnAbsencePlanForEmployee() throws Exception {
        String bootstrap=mvc.perform(get("/api/hotel/bootstrap").with(httpBasic("mariana","demo1234")))
                .andReturn().getResponse().getContentAsString();
        long employeeId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(bootstrap).get("me").get("id").asLong();
        mvc.perform(post("/api/management/plans").with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeIds":[%d],"date":"2026-07-12","kind":"VACATION","notes":"Concediu aprobat"}
                                """.formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].kind").value("VACATION"));
    }
}
