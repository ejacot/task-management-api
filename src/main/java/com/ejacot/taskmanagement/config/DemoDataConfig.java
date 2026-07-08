package com.ejacot.taskmanagement.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejacot.taskmanagement.hotel.*;
import com.ejacot.taskmanagement.user.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Configuration
@Profile("local")
public class DemoDataConfig {
    @Bean
    @Order(1)
    ApplicationRunner demoData(HotelRepository hotels, UserAccountRepository users,
                               WorkTypeRepository workTypes, ShiftPlanRepository plans,
                               WorkLogRepository logs,PayRateRepository payRates,NotificationRepository notifications, PasswordEncoder encoder) {
        return args -> {
            if (users.findByUsername("mariana").isPresent()) {
                Hotel existing=users.findByUsername("mariana").orElseThrow().getHotel();
                if(users.findByUsername("checker").isEmpty())users.save(new UserAccount("checker",encoder.encode("checker1234"),"checker@example.com","+4915100000003",UserRole.CHECKER,BigDecimal.ZERO,existing));
                if(users.findByUsername("angajator").isEmpty())users.save(new UserAccount("angajator",encoder.encode("admin1234"),"admin@example.com","+4915100000004",UserRole.EMPLOYER,BigDecimal.ZERO,existing));
                if(payRates.findFirstByEmployeeUsernameAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc("mariana",LocalDate.now()).isEmpty())payRates.save(new PayRate(users.findByUsername("mariana").orElseThrow(),new BigDecimal("17.25"),LocalDate.now().withDayOfYear(1)));
                return;
            }
            Hotel hotel = hotels.save(new Hotel("Infinity Hotel", "Unterschleißheim"));
            UserAccount employee = users.save(new UserAccount("mariana", encoder.encode("demo1234"),
                    "mariana@example.com", "+4915112345678", UserRole.EMPLOYEE, new BigDecimal("17.25"), hotel));
            employee.configureProfile("Mariana","Jacot","Unterschleißheim, Germania",1);
            users.save(new UserAccount("manager", encoder.encode("manager1234"),
                    "manager@example.com", "+4915198765432", UserRole.MANAGER, BigDecimal.ZERO, hotel));
            users.save(new UserAccount("checker",encoder.encode("checker1234"),"checker@example.com","+4915100000003",UserRole.CHECKER,BigDecimal.ZERO,hotel));
            users.save(new UserAccount("angajator",encoder.encode("admin1234"),"admin@example.com","+4915100000004",UserRole.EMPLOYER,BigDecimal.ZERO,hotel));
            payRates.save(new PayRate(employee,new BigDecimal("17.25"),LocalDate.now().withDayOfYear(1)));

            WorkType normal = workTypes.save(new WorkType(hotel, "ROOM_NORMAL", "Camere Normal", WorkUnit.ROOMS, new BigDecimal("2.40"), "#17806D"));
            WorkType junior = workTypes.save(new WorkType(hotel, "ROOM_JUNIOR", "Junior Suite", WorkUnit.ROOMS, new BigDecimal("1.60"), "#B67A2D"));
            WorkType president = workTypes.save(new WorkType(hotel, "ROOM_PRESIDENT", "President Suite", WorkUnit.ROOMS, new BigDecimal("1.20"), "#7D5BA6"));
            WorkType publicArea = workTypes.save(new WorkType(hotel, "PUBLIC", "Public Area", WorkUnit.HOURLY, null, "#3B6EA8"));
            publicArea.configureDefaults(LocalTime.of(5,0),LocalTime.of(13,30),30);
            WorkType housekeeping = workTypes.save(new WorkType(hotel, "HSK", "Housekeeping", WorkUnit.HOURLY, null, "#D05D4E"));

            LocalDate today = LocalDate.now();
            LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            plans.saveAll(List.of(
                    new ShiftPlan(employee, hotel, normal, monday, LocalTime.of(8, 0), LocalTime.of(16, 30), "Echipă cu Ana"),
                    new ShiftPlan(employee, hotel, publicArea, monday.plusDays(1), LocalTime.of(6, 0), LocalTime.of(15, 30), "Lobby și spații comune"),
                    new ShiftPlan(employee, hotel, junior, monday.plusDays(2), LocalTime.of(8, 0), LocalTime.of(16, 30), "Plan preliminar"),
                    new ShiftPlan(employee, hotel, housekeeping, monday.plusDays(4), LocalTime.of(9, 0), LocalTime.of(17, 30), null),
                    new ShiftPlan(employee, hotel, president, monday.plusDays(6), LocalTime.of(9, 0), LocalTime.of(15, 0), "Etajul superior")
            ));

            for (int i = 1; i <= 6; i++) {
                LocalDate date = today.minusDays(i * 2L);
                WorkLog log = new WorkLog(employee, hotel, i % 2 == 0 ? normal : publicArea, date,
                        i % 2 == 0 ? null : LocalTime.of(7, 0), i % 2 == 0 ? null : LocalTime.of(15, 30),
                        i % 2 == 0 ? 0 : 30, i % 2 == 0 ? RoomType.NORMAL : null,
                        i % 2 == 0 ? 12 + i : null, i % 2 == 0 ? BigDecimal.valueOf(12 + i).divide(new BigDecimal("2.40"), 2, java.math.RoundingMode.HALF_UP) : new BigDecimal("8.00"), null);
                log.submit();
                logs.save(log);
            }
            notifications.save(new Notification(employee,"Planul săptămânii este disponibil","Programul pentru săptămâna curentă a fost publicat.","plan"));
        };
    }

    @Bean
    @Order(2)
    ApplicationRunner weeklyTeamDemo(UserAccountRepository users,WorkTypeRepository types,
                                     ShiftPlanRepository plans,PasswordEncoder encoder){
        return args->{
            if(users.findByUsername("imbrea.daniela").isPresent())return;
            UserAccount mariana=users.findByUsername("mariana").orElseThrow();
            Hotel hotel=mariana.getHotel();
            WorkType ch=type(types,hotel,"CH","Checker / CH","#af52de");
            WorkType pf=type(types,hotel,"PF","Public Früh","#34c759");
            WorkType ps=type(types,hotel,"PS","Public Spät","#007aff");
            WorkType tk=type(types,hotel,"TK","Tageskraft","#ff9500");
            WorkType hd=type(types,hotel,"HD","Housekeeping Dienst","#ff3b30");
            WorkType spa=type(types,hotel,"SPA","SPA","#b38b00");
            WorkType obj=type(types,hotel,"OBJ","Objektleitung","#5856d6");
            WorkType tlw=type(types,hotel,"TLW","Teilwäsche","#5ac8fa");
            String[][] people={{"imbrea.daniela","Daniela","Imbrea"},{"cosaru.sebastian","Sebastian","Cosaru"},{"lazar.daniel","Daniel","Lazar"},{"amza.razvan","Razvan","Amza"},{"cosaru.cristina","Cristina","Cosaru"},{"ungureanu.valerica","Valerica","Ungureanu"},{"mitrea.iulian","Iulian","Mitrea"},{"mitrea.corina","Corina","Mitrea"},{"mircea.lucian","Lucian","Mircea"},{"jacot.victor","Victor","Jacot"},{"chiorpec.gertrud","Gertrud","Chiorpec"},{"balan.bianca","Bianca","Balan"},{"stancu.marian","Marian","Stancu"},{"puie.liviu","Liviu","Puie"},{"ghitescu.adrian","Adrian","Ghitescu"},{"nechita.denia","Denisa","Nechita"},{"toma.adriana","Adriana","Toma"},{"toma.viorel","Viorel","Toma"},{"lepadatu.cristian","Cristian","Lepadatu"},{"cortel.florentina","Florentina","Cortel"}};
            List<UserAccount> team=new java.util.ArrayList<>();
            for(int i=0;i<people.length;i++){
                String[] p=people[i];UserAccount u=new UserAccount(p[0],encoder.encode("demo1234"),p[0]+"@example.com","+49152000"+String.format("%04d",i),UserRole.EMPLOYEE,new BigDecimal("16.00"),hotel);u.configureProfile(p[1],p[2],"Unterschleißheim, Germania",1);team.add(users.save(u));
            }
            LocalDate monday=LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue()-1L);
            plans.deleteAll(plans.findAllByHotelIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(hotel.getId(),monday,monday.plusDays(6)));
            WorkType[] rotation={pf,ps,tk,hd,spa,obj,tlw};
            for(int i=0;i<team.size();i++)for(int d=0;d<7;d++){
                UserAccount u=team.get(i);LocalDate date=monday.plusDays(d);
                if((i+d)%9==0)plans.save(new ShiftPlan(u,hotel,null,date,null,null,null,ShiftKind.VACATION));
                else if((i+d)%6==0)plans.save(new ShiftPlan(u,hotel,null,date,null,null,null,ShiftKind.FREE));
                else{WorkType t=rotation[i%rotation.length];LocalTime start=t==pf?LocalTime.of(5,0):t==ps?LocalTime.of(13,30):t==tk?LocalTime.of(10,0):t==spa?LocalTime.of(12,0):LocalTime.of(9,0);plans.save(new ShiftPlan(u,hotel,t,date,start,start.plusHours(8).plusMinutes(30),null));}
            }
            for(int d=0;d<7;d++)plans.save(new ShiftPlan(mariana,hotel,ch,monday.plusDays(d),d<5?LocalTime.of(9,0):LocalTime.of(10,0),d<5?LocalTime.of(17,30):LocalTime.of(18,30),d<5?"CH":"Liste + CH"));
        };
    }

    @Bean
    @Order(3)
    ApplicationRunner marianaHistory2025(UserAccountRepository users, WorkTypeRepository types,
                                         WorkLogRepository logs, ObjectMapper objectMapper) {
        return args -> {
            String marker = "Import Excel Mariana 2025";
            if (logs.existsByEmployeeUsernameAndNotesContaining("mariana", marker)) return;
            UserAccount mariana = users.findByUsername("mariana").orElseThrow();
            UserAccount reviewer = users.findByUsername("manager").orElseThrow();
            Hotel hotel = mariana.getHotel();
            try (var input = new ClassPathResource("demo/mariana-2025-history.json").getInputStream()) {
                List<HistoryEntry> entries = objectMapper.readValue(input, new TypeReference<>() {});
                for (HistoryEntry entry : entries) {
                    WorkType workType = type(types, hotel, entry.code(), entry.taskName(), "#667085");
                    WorkLog log = new WorkLog(mariana, hotel, workType, entry.date(), entry.startTime(),
                            entry.endTime(), 0, null, null, entry.hours(), entry.notes());
                    log.submit();
                    log.review(reviewer, true, null);
                    logs.save(log);
                }
            }
        };
    }

    @Bean
    @Order(4)
    ApplicationRunner marianaHistory2026(UserAccountRepository users, WorkTypeRepository types,
                                         WorkLogRepository logs, ObjectMapper objectMapper) {
        return args -> {
            String marker = "Import Excel Mariana 2026";
            if (logs.existsByEmployeeUsernameAndNotesContaining("mariana", marker)) return;
            UserAccount mariana = users.findByUsername("mariana").orElseThrow();
            UserAccount reviewer = users.findByUsername("manager").orElseThrow();
            Hotel hotel = mariana.getHotel();
            logs.deleteAll(logs.findAllByEmployeeUsernameAndWorkDateBetweenOrderByWorkDateDesc(
                    "mariana", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
            try (var input = new ClassPathResource("demo/mariana-2026-history.json").getInputStream()) {
                List<HistoryEntry> entries = objectMapper.readValue(input, new TypeReference<>() {});
                for (HistoryEntry entry : entries) {
                    WorkType workType = type(types, hotel, entry.code(), entry.taskName(), "#1768e5");
                    WorkLog log = new WorkLog(mariana, hotel, workType, entry.date(), entry.startTime(),
                            entry.endTime(), 0, null, null, entry.hours(), entry.notes());
                    log.submit();
                    log.review(reviewer, true, null);
                    logs.save(log);
                }
            }
        };
    }

    @Bean
    @Order(5)
    ApplicationRunner plannedLogsDemo(ShiftPlanRepository plans,PlanLogService planLogs){
        return args->{LocalDate today=LocalDate.now();plans.findAll().stream().filter(plan->!plan.getWorkDate().isBefore(today)).map(ShiftPlan::getId).forEach(planLogs::ensureForId);};
    }

    private record HistoryEntry(LocalDate date, String code, String taskName, BigDecimal hours,
                                LocalTime startTime, LocalTime endTime, String notes) {}

    private WorkType type(WorkTypeRepository repo,Hotel hotel,String code,String name,String color){return repo.findByHotelIdAndCode(hotel.getId(),code).orElseGet(()->repo.save(new WorkType(hotel,code,name,WorkUnit.HOURLY,null,color)));}
}
