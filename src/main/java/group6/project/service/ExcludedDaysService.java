package group6.project.service;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.repo.ExcludedDaysRepo;
import java.util.List;
import group6.project.model.ExcludedDays;

@Service
public class ExcludedDaysService {

    @Autowired
private ExcludedDaysRepo excludedDaysRepo;

    public boolean isExcludedDay(LocalDate date) {
        return excludedDaysRepo.existsByHolidayDate(date);
    }
    public List<ExcludedDays> getAllExcludedDays() {
        return excludedDaysRepo.findAll();
}
    public ExcludedDays getExcludedDayById(Long id) {
    return excludedDaysRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Excluded day not found with id: " + id));
}

    public ExcludedDays addExcludedDay(ExcludedDays excludedDay) {
        if (excludedDaysRepo.existsByHolidayDate(excludedDay.getHolidayDate
            ())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
        "This holiday date already exists.");
    }
        return excludedDaysRepo.save(excludedDay);
}
    public ExcludedDays updateExcludedDay(Long id, ExcludedDays excludedDay) {

    ExcludedDays existingDay = excludedDaysRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                   HttpStatus.NOT_FOUND,
                   "Excluded day not found with id: " + id));
    
    if (excludedDaysRepo.existsByHolidayDateAndIdNot(
        excludedDay.getHolidayDate(), id)) {
       throw new RuntimeException("This holiday date already exists.");
}

    existingDay.setHolidayDate(excludedDay.getHolidayDate());
    existingDay.setDescription(excludedDay.getDescription());

    return excludedDaysRepo.save(existingDay);
}
    public void deleteExcludedDay(Long id) {
        if (!excludedDaysRepo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Excluded day not found with id: " + id);
}
        excludedDaysRepo.deleteById(id);
}
  
}
