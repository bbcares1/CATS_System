package group6.project;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import group6.project.repo.StaffRepo;
import group6.project.repo.ManagerRepo;
import group6.project.service.StaffService;
import group6.project.service.ManagerService;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendIntegrationTest {
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @Autowired StaffRepo staffRepo;
    @Autowired ManagerRepo managerRepo;
    @Autowired StaffService staffService;
    @Autowired ManagerService managerService;

    @Test
    void seedDataResolvesRealStaffAndManagerAccounts() throws Exception {
        var staff = staffService.requireStaff(() -> "staff_charlie", null);
        var manager = managerService.requireManager(() -> "mgr_bob", null);
        assertEquals(200, getBudgetEdit(staff.getUserId()));
        assertEquals(5, staffRepo.count());
        assertEquals(1, managerRepo.count());
        assertEquals(manager.getUserId(), staff.getManager().getUserId());
        assertEquals(3000d, staff.getTrainingBudget());
    }

    @Test
    void pagesAndRepositoryBackedApisRespondSuccessfully() throws Exception {
        for (String path : new String[] {"/api/staff", "/api/managers", "/admin/showBudgetList",
                "/admin/courses", "/admin/hierarchy", "/admin/categories", "/excluded-days",
                "/course-fee-applications", "/course-fee-applications/new"}) {
            assertEquals(200, get(path).statusCode(), path);
        }
    }

    @Test
    void managerQueueRequiresIdentity() throws Exception {
        assertEquals(401, get("/api/managers/me/course-applications").statusCode());
    }

    private int getBudgetEdit(Integer id) throws Exception {
        return get("/admin/update/" + id).statusCode();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
