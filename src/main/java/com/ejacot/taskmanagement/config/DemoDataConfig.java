package com.ejacot.taskmanagement.config;

import com.ejacot.taskmanagement.hotel.*;
import com.ejacot.taskmanagement.user.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Configuration
@Profile("local")
public class DemoDataConfig {
    @Bean
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
}
