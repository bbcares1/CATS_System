package group6.project.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import group6.project.model.ExcludedDays;
import group6.project.repo.ExcludedDaysRepo;

@Service
public class ExcludedDaysService {

    @Autowired
    private ExcludedDaysRepo excludedDaysRepo;

    public boolean isExcludedDay(LocalDate date) {
        return excludedDaysRepo.existsByDate(date);
    }

    public List<ExcludedDays> getAllExcludedDays() {
        return excludedDaysRepo.findAll();
    }

    public ExcludedDays getExcludedDayById(Integer id) {
        return excludedDaysRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Excluded day not found with id: " + id));
    }

    public ExcludedDays addExcludedDay(ExcludedDays excludedDay) {

        if (excludedDaysRepo.existsByDate(excludedDay.getDate())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This holiday date already exists.");
        }

        return excludedDaysRepo.save(excludedDay);
    }

    public ExcludedDays updateExcludedDay(
            Integer id,
            ExcludedDays excludedDay) {

        ExcludedDays existingDay = excludedDaysRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Excluded day not found with id: " + id));

        if (excludedDaysRepo.existsByDateAndIdNot(
                excludedDay.getDate(), id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This holiday date already exists.");
        }

        existingDay.setDate(excludedDay.getDate());
        existingDay.setDescription(excludedDay.getDescription());

        return excludedDaysRepo.save(existingDay);
    }

    public void deleteExcludedDay(Integer id) {

        if (!excludedDaysRepo.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Excluded day not found with id: " + id);
        }

        excludedDaysRepo.deleteById(id);
    }
}
