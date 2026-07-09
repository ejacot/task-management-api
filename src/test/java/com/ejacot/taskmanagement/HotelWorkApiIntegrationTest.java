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
        mvc.perform(post("/api/management/plans/copy-week").with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceMonday\":\"2026-07-06\",\"targetMonday\":\"2026-07-13\",\"overwrite\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.date == '2026-07-19')].kind").value(org.hamcrest.Matchers.hasItem("VACATION")));
    }

    @Test
    void plannedWorkBecomesApprovedLogAndOnlyCorrectionsNeedReview() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var bootstrap=mapper.readTree(mvc.perform(get("/api/hotel/bootstrap").with(httpBasic("mariana","demo1234"))).andReturn().getResponse().getContentAsString());
        long employeeId=bootstrap.get("me").get("id").asLong();
        long hourlyTypeId=java.util.stream.StreamSupport.stream(bootstrap.get("workTypes").spliterator(),false)
                .filter(type->"HOURLY".equals(type.get("unit").asText())).findFirst().orElseThrow().get("id").asLong();
        var plan=mapper.readTree(mvc.perform(post("/api/management/plans").with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"employeeIds":[%d],"workTypeId":%d,"date":"2026-08-20","startTime":"09:00","endTime":"17:30","kind":"WORK"}
                        """.formatted(employeeId,hourlyTypeId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get(0);
        long planId=plan.get("id").asLong();
        var afterPlan=mapper.readTree(mvc.perform(get("/api/hotel/bootstrap").with(httpBasic("mariana","demo1234"))).andReturn().getResponse().getContentAsString());
        var plannedLog=java.util.stream.StreamSupport.stream(afterPlan.get("logs").spliterator(),false)
                .filter(log->log.hasNonNull("shiftPlanId")&&log.get("shiftPlanId").asLong()==planId).findFirst().orElseThrow();
        long logId=plannedLog.get("id").asLong();
        org.assertj.core.api.Assertions.assertThat(plannedLog.get("status").asText()).isEqualTo("APPROVED");
        org.assertj.core.api.Assertions.assertThat(plannedLog.get("hours").decimalValue()).isEqualByComparingTo("8.00");

        mvc.perform(put("/api/hotel/logs/{id}/correction",logId).with(httpBasic("mariana","demo1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"startTime":"09:00","endTime":"18:30","breakMinutes":30,"reason":"Am lucrat o oră în plus"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED")).andExpect(jsonPath("$.hours").value(9.00));
        mvc.perform(put("/api/management/logs/{id}/review",logId).with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.hours").value(9.00));
        mvc.perform(put("/api/hotel/logs/{id}/correction",logId).with(httpBasic("mariana","demo1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"startTime":"08:00","endTime":"18:30","breakMinutes":30,"reason":"A doua corectare"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(put("/api/management/logs/{id}/review",logId).with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false,\"reason\":\"Planul inițial este corect\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.hours").value(8.00));
    }

    @Test
    void employeeCanCreateRequestAndLoadPayroll() throws Exception {
        String created=mvc.perform(post("/api/employee/requests").with(httpBasic("mariana","demo1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"SCHEDULE_CHANGE","startDate":"2026-08-21","endDate":"2026-08-21","message":"Am nevoie de alt program"}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asLong();
        mvc.perform(get("/api/employee/management/requests").with(httpBasic("manager","manager1234")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(put("/api/employee/management/requests/{id}",id).with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"response\":\"Aprobat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/employee/payroll/2026").with(httpBasic("mariana","demo1234")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.months.length()").value(12));
    }

    @Test
    void approvedVacationRequestAutomaticallyUpdatesTheEmployeePlan() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        String created=mvc.perform(post("/api/employee/requests").with(httpBasic("mariana","demo1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"VACATION","startDate":"2026-09-14","endDate":"2026-09-16","message":"Concediu"}
                        """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id=mapper.readTree(created).get("id").asLong();
        mvc.perform(put("/api/employee/management/requests/{id}",id).with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"response\":\"Concediu aprobat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(get("/api/hotel/bootstrap").with(httpBasic("mariana","demo1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans[?(@.date == '2026-09-14')].kind").value(org.hamcrest.Matchers.hasItem("VACATION")))
                .andExpect(jsonPath("$.plans[?(@.date == '2026-09-15')].kind").value(org.hamcrest.Matchers.hasItem("VACATION")))
                .andExpect(jsonPath("$.plans[?(@.date == '2026-09-16')].kind").value(org.hamcrest.Matchers.hasItem("VACATION")));
    }

    @Test
    void managerCanAssignRoomsAndEmployeeCanOnlyReadTheirList() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        var bootstrap=mapper.readTree(mvc.perform(get("/api/hotel/bootstrap").with(httpBasic("mariana","demo1234"))).andReturn().getResponse().getContentAsString());
        long employeeId=bootstrap.get("me").get("id").asLong();
        mvc.perform(post("/api/management/rooms").with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"employeeIds":[%d],"date":"2026-10-05","rooms":[{"number":"301","category":"NORMAL"},{"number":"801","category":"PRESIDENT"}]}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/employee/rooms?from=2026-10-05&to=2026-10-05").with(httpBasic("mariana","demo1234")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].roomNumber").value("301")).andExpect(jsonPath("$[1].roomNumber").value("801"));
        mvc.perform(post("/api/management/rooms").with(httpBasic("mariana","demo1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"employeeIds":[%d],"date":"2026-10-06","rooms":[{"number":"302","category":"NORMAL"}]}
                        """.formatted(employeeId))).andExpect(status().isForbidden());
    }

    @Test
    void tokenLoginRoomCompletionCheckerAndPayrollExportWork() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        String login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"login":"mariana","password":"demo1234"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.token").exists()).andReturn().getResponse().getContentAsString();
        String token=mapper.readTree(login).get("token").asText();
        var bootstrap=mapper.readTree(mvc.perform(get("/api/hotel/bootstrap").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long employeeId=bootstrap.get("me").get("id").asLong();
        String assigned=mvc.perform(post("/api/management/rooms").with(httpBasic("manager","manager1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"employeeIds":[%d],"date":"2026-11-02","rooms":[{"number":"401","category":"NORMAL"}]}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long roomId=mapper.readTree(assigned).get(0).get("id").asLong();
        mvc.perform(put("/api/employee/rooms/{id}/complete",roomId).header("Authorization","Bearer "+token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Gata\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/checker/rooms?date=2026-11-02").with(httpBasic("checker","checker1234")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(1));
        mvc.perform(put("/api/checker/rooms/{id}",roomId).with(httpBasic("checker","checker1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DEFECT\",\"defectDescription\":\"Prosop lipsa\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEFECT")).andExpect(jsonPath("$.defectStatus").value("OPEN"));
        mvc.perform(put("/api/checker/rooms/{id}/defect",roomId).with(httpBasic("checker","checker1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"notes\":\"Se rezolva\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.defectStatus").value("IN_PROGRESS"));
        mvc.perform(put("/api/checker/rooms/{id}/defect",roomId).with(httpBasic("checker","checker1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"notes\":\"Rezolvat\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CHECKED")).andExpect(jsonPath("$.defectStatus").value("RESOLVED"));
        mvc.perform(get("/api/admin/payroll/export.csv?year=2026&month=1").with(httpBasic("angajator","admin1234")))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("payroll-2026-1.csv")));
    }

    @Test
    void excelPreviewAndStaticFrontendAssetsWork() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); var out = new java.io.ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Plan");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Name Vorname");
            header.createCell(1).setCellValue("2026-12-01");
            header.createCell(2).setCellValue("2026-12-02");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("Test Angajat");
            row.createCell(1).setCellValue("CH 09:00-17:30");
            row.createCell(2).setCellValue("F");
            workbook.write(out);
            var file = new org.springframework.mock.web.MockMultipartFile("file","plan.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",out.toByteArray());
            mvc.perform(multipart("/api/admin/imports/plans/preview").file(file).with(httpBasic("manager","manager1234")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employees").value(1))
                    .andExpect(jsonPath("$.planCells").value(2))
                    .andExpect(jsonPath("$.rows[0].employee").value("Test Angajat"));
        }
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/admin-checker.js")).andExpect(status().isOk());
        mvc.perform(get("/manifest.webmanifest")).andExpect(status().isOk()).andExpect(jsonPath("$.short_name").value("Roomly"));
    }
}
