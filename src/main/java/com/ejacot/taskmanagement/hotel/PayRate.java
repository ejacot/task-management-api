package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.UserAccount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="pay_rates")
public class PayRate {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="employee_id") private UserAccount employee;
    @Column(name="hourly_rate",nullable=false,precision=10,scale=2) private BigDecimal hourlyRate;
    @Column(name="effective_from",nullable=false) private LocalDate effectiveFrom;
    @Column(name="effective_to") private LocalDate effectiveTo;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected PayRate(){}
    public PayRate(UserAccount employee,BigDecimal hourlyRate,LocalDate effectiveFrom){this.employee=employee;this.hourlyRate=hourlyRate;this.effectiveFrom=effectiveFrom;this.createdAt=Instant.now();}
    public BigDecimal getHourlyRate(){return hourlyRate;} public LocalDate getEffectiveFrom(){return effectiveFrom;}
}

