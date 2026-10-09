package group6.project.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import group6.project.model.CourseBatch;
import group6.project.repo.CourseBatchRepo;

@Service
public class CourseBatchService {
  private final CourseBatchRepo courseBatchRepo;

  public CourseBatchService(CourseBatchRepo courseBatchRepo){
    this.courseBatchRepo = courseBatchRepo;
  }

  public List<CourseBatch> getAllBatches(){
    return courseBatchRepo.findAll();
  }

  public Optional<CourseBatch> getBatchById(Long batchId){
    return courseBatchRepo.findById(batchId);
  }

  public CourseBatch createBatch(CourseBatch courseBatch){
    validateBatch(courseBatch);
    return courseBatchRepo.save(courseBatch);
  }

  public void deleteBatch(Long batchId){
    courseBatchRepo.deleteById(batchId);
  }

  public Optional<CourseBatch> updateBatch(
        CourseBatch courseBatch,
        Long batchId) {

    Optional<CourseBatch> existingBatch =
            courseBatchRepo.findById(batchId);

    if (existingBatch.isPresent()) {

        CourseBatch existing = existingBatch.get();

        existing.setCapacity(
            courseBatch.getCapacity()
        );

        existing.setCourseStartDate(
            courseBatch.getCourseStartDate()
        );

        existing.setCourseEndDate(
            courseBatch.getCourseEndDate()
        );

        existing.setTrainingDays(
            courseBatch.getTrainingDays()
        );

        validateBatch(existing);

        CourseBatch saved =
                courseBatchRepo.save(existing);

        return Optional.of(saved);
    }

    return Optional.empty();
}


  //Validation
  private void validateBatch(CourseBatch courseBatch){
    if (courseBatch.getCourseDetail() == null){
      throw new IllegalArgumentException("Course is required");
    }

    if (courseBatch.getCourseStartDate() == null || courseBatch.getCourseEndDate() == null){
      throw new IllegalArgumentException("Start date and end date are required");
    }

    if (courseBatch.getCourseEndDate().isBefore(courseBatch.getCourseStartDate())){
      throw new IllegalArgumentException("End date cannot be before start date");
    }

    Double trainingDays = courseBatch.getTrainingDays();
    if (trainingDays == null || !Double.isFinite(trainingDays)
        || trainingDays <= 0 || trainingDays % 0.5 != 0) {
      throw new IllegalArgumentException(
          "Training days must be a positive whole or half-day amount"
      );
    }

    if (courseBatch.getCapacity() == null ||
    courseBatch.getCapacity() <= 0) {

    throw new IllegalArgumentException(
        "Capacity must be greater than 0"
    );
}
  }
}
