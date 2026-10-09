package group6.project.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import group6.project.model.*;
import group6.project.repo.*;

class CourseCatalogueServiceTest {
    CourseDetailRepo courses = mock(CourseDetailRepo.class);
    CourseCatalogueService catalogue = new CourseCatalogueService(courses, mock(CourseBatchRepo.class),
            mock(CourseApplicationRepo.class), mock(CourseApplicationService.class));

    // Filters combine with publication and provider validation instead of exposing unfinished offers.
    @Test
    void searchCombinesCaseInsensitiveTextCategoryAndProvider() {
        CourseDetail java = offer("Java skills", "ISS"); CourseDetail cloud = offer("Cloud", "Academy");
        CourseDetail archived = offer("Java archived", "ISS"); archived.setActive(false);
        CourseDetail incomplete = offer("Java draft", "");
        when(courses.findAll()).thenReturn(List.of(java, cloud, archived, incomplete));
        assertEquals(List.of(java), catalogue.search("JAVA", CourseCategoryType.EXTERNAL_COURSE, "ISS"));
        assertTrue(catalogue.search("java", CourseCategoryType.INTERNAL_TRAINING, "ISS").isEmpty());
    }

    // Archived links return 404 rather than allowing a new application against an old offer.
    @Test
    void archivedOfferCannotBeAppliedFor() {
        CourseDetail old = offer("Old", "ISS"); old.setActive(false); when(courses.findById(1)).thenReturn(java.util.Optional.of(old));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> catalogue.offer(1)).getStatusCode().value());
    }

    // Published test offers carry the same required metadata as Admin-created offers.
    private CourseDetail offer(String title, String provider) {
        CourseCategory category = new CourseCategory(); category.setKind(CourseCategoryType.EXTERNAL_COURSE);
        CourseDetail course = new CourseDetail(); course.setTitle(title); course.setTrainingProvider(provider);
        course.setCourseCategory(category); course.setCourseFee(BigDecimal.TEN); return course;
    }
}
