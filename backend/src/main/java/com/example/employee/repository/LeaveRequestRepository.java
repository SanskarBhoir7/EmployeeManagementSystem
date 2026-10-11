package com.example.employee.repository;

import com.example.employee.entity.LeaveRequest;
import com.example.employee.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status);

    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE " +
           "l.employee.id = :empId AND " +
           "l.status IN :statuses AND " +
           "l.startDate <= :endDate AND " +
           "l.endDate >= :startDate")
    boolean existsOverlappingLeave(@Param("empId") Long empId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("statuses") Collection<LeaveStatus> statuses);

    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l WHERE " +
           "l.employee.id = :empId AND " +
           "l.id <> :excludeId AND " +
           "l.status IN :statuses AND " +
           "l.startDate <= :endDate AND " +
           "l.endDate >= :startDate")
    boolean existsOverlappingLeaveExcludingId(@Param("empId") Long empId,
                                            @Param("excludeId") Long excludeId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("statuses") Collection<LeaveStatus> statuses);

    long countByStatus(LeaveStatus status);
}
