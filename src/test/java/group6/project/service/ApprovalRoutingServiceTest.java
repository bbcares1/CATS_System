package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import group6.project.model.*;
import group6.project.repo.UserRepo;

class ApprovalRoutingServiceTest {
    UserRepo users=mock(UserRepo.class);
    ApprovalRoutingService routing=new ApprovalRoutingService(users);

    // Ordinary employees cannot choose another reviewer instead of their assigned manager.
    @Test void staffCannotBypassReportingManager() {
        Staff employee=staff(2); Manager boss=manager(1); employee.setManager(boss);
        when(users.reportingManagerId(2)).thenReturn(Optional.of(1));
        when(users.lockParticipants(List.of(1,2))).thenReturn(List.of(boss,employee));
        assertThrows(IllegalArgumentException.class,()->routing.forSubmission(employee,3));
        assertSame(boss,routing.forSubmission(employee,null));
    }

    // A root Manager can select a peer without inventing a cyclic reporting assignment.
    @Test void unassignedManagerCanChoosePeer() {
        Manager employee=manager(2),peer=manager(1);
        when(users.lockParticipants(List.of(1,2))).thenReturn(List.of(peer,employee));
        assertSame(peer,routing.forSubmission(employee,1)); assertNull(employee.getManager());
    }

    // Staff with no reporting assignment must be configured before submitting.
    @Test void unassignedStaffCannotChoosePeer() {
        Staff employee=staff(2); Manager peer=manager(1);
        when(users.lockParticipants(List.of(1,2))).thenReturn(List.of(peer,employee));
        assertThrows(IllegalArgumentException.class,()->routing.forSubmission(employee,1));
    }

    // Neither disabled accounts nor self-review can create a request.
    @Test void disabledAndSelfReviewAreRejected() {
        Manager employee=manager(2); when(users.lockParticipants(List.of(2))).thenReturn(List.of(employee));
        assertThrows(IllegalArgumentException.class,()->routing.forSubmission(employee,2));
        employee.setActive(false); assertThrows(IllegalArgumentException.class,()->routing.forSubmission(employee,2));
    }

    // Participant lock order is stable even when a higher-ID employee chooses a lower-ID peer.
    @Test void locksUseSortedDistinctIdentities() {
        when(users.lockParticipants(List.of(2,9))).thenReturn(List.of(staff(2),manager(9)));
        assertEquals(2,routing.lockParticipants(9,2).size()); verify(users).lockParticipants(List.of(2,9));
    }

    // Small isolated identities keep the tests focused on routing, not persistence.
    private Staff staff(int id) { Staff user=new Staff(); user.setUserId(id); return user; }
    private Manager manager(int id) { Manager user=new Manager(); user.setUserId(id); return user; }
}
