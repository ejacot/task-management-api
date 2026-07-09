package com.ejacot.taskmanagement.user;

import com.ejacot.taskmanagement.hotel.Hotel;
import com.ejacot.taskmanagement.hotel.UserRole;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(unique = true, length = 150)
    private String email;

    @Column(unique = true, length = 30)
    private String phone;

    @Column(name="first_name",length=80) private String firstName;
    @Column(name="last_name",length=80) private String lastName;
    @Column(length=255) private String address;
    @Column(name="steuer_class") private Integer steuerClass;
    @Column(name="team_name",length=120) private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role = UserRole.EMPLOYEE;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
    @Column(name="deactivated_at") private Instant deactivatedAt;
    @Column(name="invitation_token",length=80) private String invitationToken;
    @Column(name="invitation_expires_at") private Instant invitationExpiresAt;
    @Column(name="password_reset_code",length=20) private String passwordResetCode;
    @Column(name="password_reset_expires_at") private Instant passwordResetExpiresAt;
    @Column(name="failed_login_attempts",nullable=false) private int failedLoginAttempts;
    @Column(name="locked_until") private Instant lockedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
        this.createdAt = Instant.now();
    }

    public UserAccount(String username, String password, String email, String phone,
                       UserRole role, BigDecimal hourlyRate, Hotel hotel) {
        this(username, password);
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.hourlyRate = hourlyRate;
        this.hotel = hotel;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getAddress(){return address;} public Integer getSteuerClass(){return steuerClass;} public String getTeamName(){return teamName;}
    public UserRole getRole() { return role; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public boolean isActive() { return active; }
    public Hotel getHotel() { return hotel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeactivatedAt(){return deactivatedAt;} public String getInvitationToken(){return invitationToken;} public Instant getInvitationExpiresAt(){return invitationExpiresAt;} public String getPasswordResetCode(){return passwordResetCode;} public Instant getPasswordResetExpiresAt(){return passwordResetExpiresAt;}
    public int getFailedLoginAttempts(){return failedLoginAttempts;} public Instant getLockedUntil(){return lockedUntil;} public boolean isLocked(){return lockedUntil!=null&&lockedUntil.isAfter(Instant.now());}
    public void configureProfile(String firstName,String lastName,String address,Integer steuerClass){this.firstName=firstName;this.lastName=lastName;this.address=address;this.steuerClass=steuerClass;}
    public void updateProfile(String firstName,String lastName,String email,String phone,String address,Integer steuerClass){this.firstName=firstName;this.lastName=lastName;this.email=email;this.phone=phone;this.address=address;this.steuerClass=steuerClass;}
    public void changePassword(String password){this.password=password;}
    public void adminUpdate(String username,String firstName,String lastName,String email,String phone,String address,Integer steuerClass,UserRole role,BigDecimal hourlyRate,Hotel hotel,String teamName){this.username=username;this.firstName=firstName;this.lastName=lastName;this.email=email;this.phone=phone;this.address=address;this.steuerClass=steuerClass;this.role=role;this.hourlyRate=hourlyRate;this.hotel=hotel;this.teamName=teamName;}
    public void activate(){this.active=true;this.deactivatedAt=null;}
    public void deactivate(){this.active=false;this.deactivatedAt=Instant.now();}
    public void invite(String token,Instant expiresAt){this.invitationToken=token;this.invitationExpiresAt=expiresAt;}
    public void acceptInvitation(String password){this.password=password;this.active=true;this.invitationToken=null;this.invitationExpiresAt=null;}
    public void requestPasswordReset(String code,Instant expiresAt){this.passwordResetCode=code;this.passwordResetExpiresAt=expiresAt;}
    public void resetPassword(String password){this.password=password;this.passwordResetCode=null;this.passwordResetExpiresAt=null;}
    public void registerLoginFailure(){this.failedLoginAttempts++;if(this.failedLoginAttempts>=5)this.lockedUntil=Instant.now().plusSeconds(900);}
    public void registerLoginSuccess(){this.failedLoginAttempts=0;this.lockedUntil=null;}
}
