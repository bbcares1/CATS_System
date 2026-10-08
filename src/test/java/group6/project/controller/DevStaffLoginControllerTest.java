package group6.project.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import group6.project.model.Staff;
import group6.project.repo.StaffRepo;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class DevStaffLoginControllerTest {
    @Test
    void createsStaffSessionAndInvalidatesPreviousIdentity() {
        var repo = mock(StaffRepo.class);
        var staff = new Staff();
        staff.setUserName("staff_charlie");
        when(repo.findByUserName("staff_charlie")).thenReturn(Optional.of(staff));
        var request = new MockHttpServletRequest();
        var previous = request.getSession();
        previous.setAttribute("loggedInUser", "mgr_bob");
        assertEquals("redirect:/staff/course-applications",
                new DevStaffLoginController(repo).selectStaff("staff_charlie", request));
        assertNotSame(previous, request.getSession());
        assertEquals("staff_charlie", request.getSession().getAttribute("currentUser"));
        assertNull(request.getSession().getAttribute("loggedInUser"));
    }

    @Test
    void unknownAccountCannotCreateSession() {
        var repo = mock(StaffRepo.class);
        when(repo.findByUserName("unknown")).thenReturn(Optional.empty());
        var request = new MockHttpServletRequest();
        assertThrows(ResponseStatusException.class,
                () -> new DevStaffLoginController(repo).selectStaff("unknown", request));
        assertNull(request.getSession(false));
    }
}
