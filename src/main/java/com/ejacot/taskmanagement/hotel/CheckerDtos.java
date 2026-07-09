package com.ejacot.taskmanagement.hotel;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class CheckerDtos {
    private CheckerDtos() {}
    public record DayRooms(LocalDate date,int total,int assigned,int checked,int defects,int released,List<ManagementDtos.RoomAssignmentView> rooms) {}
    public record RoomReview(@NotNull RoomAssignmentStatus status,@Size(max=500) String notes,@Size(max=500) String defectDescription) {}
}
