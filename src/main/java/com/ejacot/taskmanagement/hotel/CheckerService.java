package com.ejacot.taskmanagement.hotel;

import com.ejacot.taskmanagement.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional
public class CheckerService {
 private final UserAccountRepository users;private final RoomAssignmentRepository rooms;private final NotificationRepository notifications;
 public CheckerService(UserAccountRepository users,RoomAssignmentRepository rooms,NotificationRepository notifications){this.users=users;this.rooms=rooms;this.notifications=notifications;}
 @Transactional(readOnly=true) public CheckerDtos.DayRooms rooms(String username,LocalDate date){UserAccount checker=user(username);List<RoomAssignment> items=rooms.findAllByHotelIdAndWorkDateOrderByRoomNumberAsc(hotel(checker),date);return summary(date,items);}
 public ManagementDtos.RoomAssignmentView review(String username,Long id,CheckerDtos.RoomReview r){UserAccount checker=user(username);RoomAssignment room=rooms.findByIdAndHotelId(id,hotel(checker)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(r.status()==RoomAssignmentStatus.ASSIGNED)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Alege CHECKED, DEFECT sau RELEASED");room.review(checker,r.status(),r.notes(),r.defectDescription());String title=switch(r.status()){case CHECKED->"Camera verificată";case DEFECT->"Problemă la camera "+room.getRoomNumber();case RELEASED->"Camera eliberată";default->"Camera actualizată";};notifications.save(new Notification(room.getEmployee(),title,room.getRoomNumber()+" · "+r.status(),"rooms"));return ManagementDtos.RoomAssignmentView.from(room);}
 private CheckerDtos.DayRooms summary(LocalDate date,List<RoomAssignment> items){return new CheckerDtos.DayRooms(date,items.size(),count(items,RoomAssignmentStatus.ASSIGNED),count(items,RoomAssignmentStatus.CHECKED),count(items,RoomAssignmentStatus.DEFECT),count(items,RoomAssignmentStatus.RELEASED),items.stream().map(ManagementDtos.RoomAssignmentView::from).toList());}
 private int count(List<RoomAssignment> items,RoomAssignmentStatus status){return (int)items.stream().filter(r->r.getStatus()==status).count();}
 private UserAccount user(String username){return users.findByUsername(username).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 private Long hotel(UserAccount u){if(u.getHotel()==null)throw new ResponseStatusException(HttpStatus.CONFLICT);return u.getHotel().getId();}
}
