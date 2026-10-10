package group6.project.service;

import static org.springframework.http.HttpStatus.*;

import group6.project.form.CourseProviderForm;
import group6.project.model.CourseProvider;
import group6.project.repo.CourseDetailRepo;
import group6.project.repo.CourseProviderRepo;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CourseProviderService {
    private final CourseProviderRepo providers;
    private final CourseDetailRepo courses;

    public CourseProviderService(CourseProviderRepo providers, CourseDetailRepo courses) {
        this.providers = providers;
        this.courses = courses;
    }

    // Archived providers remain visible to Admin for maintenance.
    public List<CourseProvider> all() {
        return providers.findAll(Sort.by("name"));
    }

    // Resolve bookmarks and edit links against saved records.
    public CourseProvider get(Integer id) {
        return providers
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Provider not found."));
    }

    // Prepare just the values that Admin can edit.
    public CourseProviderForm form(Integer id) {
        CourseProvider provider = get(id);
        CourseProviderForm form = new CourseProviderForm();
        form.setVersion(provider.getVersion());
        form.setName(provider.getName());
        form.setWebsite(provider.getWebsite());
        form.setEmail(provider.getEmail());
        form.setActive(provider.isActive());
        return form;
    }

    // Check duplicates before saving so the form can explain the problem.
    @Transactional
    public void save(Integer id, CourseProviderForm form) {
        CourseProvider provider = id == null ? new CourseProvider() : get(id);
        if (id != null && !Objects.equals(form.getVersion(), provider.getVersion())) {
            throw new ResponseStatusException(CONFLICT, "This provider changed. Reload it.");
        }
        String name = form.getName().trim();
        boolean duplicate =
                id == null
                        ? providers.existsByNameIgnoreCase(name)
                        : providers.existsByNameIgnoreCaseAndProviderIdNot(name, id);
        if (duplicate)
            throw new ResponseStatusException(BAD_REQUEST, "That provider name is already used.");
        provider.setName(name);
        provider.setWebsite(form.getWebsite());
        provider.setEmail(form.getEmail());
        provider.setActive(form.isActive());
        providers.save(provider);
    }

    // Archive providers still used by the catalogue; history keeps its submitted name.
    @Transactional
    public void remove(Integer id, Long version) {
        CourseProvider provider = get(id);
        if (!Objects.equals(version, provider.getVersion()))
            throw new ResponseStatusException(CONFLICT, "Reload this provider.");
        if (courses.existsByProvider_ProviderId(id)) provider.setActive(false);
        else providers.delete(provider);
    }
}
