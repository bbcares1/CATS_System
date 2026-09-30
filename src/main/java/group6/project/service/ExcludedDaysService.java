package group6.project.service;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public ExcludedDays saveExcludedDay(ExcludedDays excludedDay) {
        return excludedDaysRepo.save(excludedDay);
}
    public void deleteExcludedDay(Long id) {
        excludedDaysRepo.deleteById(id);
}
  
}
