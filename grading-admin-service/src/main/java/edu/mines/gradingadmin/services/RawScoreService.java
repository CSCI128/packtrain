package edu.mines.gradingadmin.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class RawScoreService {

    private final RawScoreRepo rawScoreRepo;

    public RawScoreService(RawScoreRepo rawScoreRepo){
        this.rawScoreRepo = rawScoreRepo;
    }

    public List<RawScore> uploadRawScores(MultipartFile file, UUID assignmentId, UUID migrationID) throws Exception {
        List<RawScore> scores = new LinkedList<>();

        if (file.isEmpty()){
            log.warn("Failed to find the file {}", file.getName());
            return scores;
        }
        if(file.getContentType() == null){
            log.warn("File content type not defined for file {}", file.getName());
            return scores;
        }
        if (!file.getContentType().equals("application/csv")){
            log.warn("File extension is not .csv for the file {}", file.getName());
            return scores;
        }

        try (InputStream inputStream = file.getInputStream()) {
            try(CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream))
                    .withSkipLines(1)
                    .build()){
                String[] line;
                while((line = csvReader.readNext()) != null){
                    try{
                        String cwid = line[2];
                        String status = line[8].trim().toUpperCase();

                        Double score = null;
                        if(!line[6].isEmpty()){
                            score = Double.parseDouble(line[6]);
                        }

                        Instant submissionTime = null;
                        if(!line[10].isEmpty()) {
                            DateTimeFormatter submissionTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
                            OffsetDateTime offsetSubmissionTime = OffsetDateTime.parse(line[10], submissionTimeFormatter);
                            submissionTime = offsetSubmissionTime.toInstant();
                        }

                        LocalTime hoursLate = null;
                        if(!line[11].isEmpty()){
                            hoursLate = LocalTime.parse(line[11]);
                        }

                        SubmissionStatus submissionStatus;
                        try {
                            submissionStatus = SubmissionStatus.valueOf(status);
                        }
                        catch (IllegalArgumentException e) {
                            log.warn("Invalid submission status {} for cwid {}. Skipping this raw score", status, cwid);
                            continue;
                        }

                        Optional<RawScore> rawScore = createRawScore(migrationID, assignmentId, cwid, score, submissionTime, hoursLate, submissionStatus);

                        if(rawScore.isEmpty()){
                            log.warn("Could not create raw score for {} on assignment {}", cwid, assignmentId);
                            continue;
                        }

                        scores.add(rawScore.get());

                    }
                    catch (Exception e){
                        log.warn(e.getMessage());
                        log.warn("Wrong input format for the csv");
                    }

                }
            }
        }

        return scores;
    }

    public Optional<RawScore> createRawScore(UUID migrationId, UUID assignmentId, String cwid, Double score, Instant submissionTime, LocalTime hoursLate, SubmissionStatus submissionStatus){
        RawScore newRawScore = rawScoreRepo.getByCwidAndAssignmentId(cwid, assignmentId)
                .map(rawScore -> {
                    log.warn("Overwriting raw score for {} with user {}", rawScore.getAssignmentId(), rawScore.getCwid());
                    rawScore.setSubmissionStatus(submissionStatus);
                    rawScore.setMigrationId(migrationId);
                    rawScore.setCwid(cwid);
                    rawScore.setScore(score);
                    rawScore.setSubmissionTime(submissionTime);
                    rawScore.setHoursLate(hoursLate);
                    rawScore.setSubmissionStatus(submissionStatus);
                    return rawScore;
                })
                .orElseGet(() -> {
                    RawScore newScore = new RawScore();
                    newScore.setMigrationId(migrationId);
                    newScore.setAssignmentId(assignmentId);
                    newScore.setCwid(cwid);
                    newScore.setScore(score);
                    newScore.setSubmissionTime(submissionTime);
                    newScore.setHoursLate(hoursLate);
                    newScore.setSubmissionStatus(submissionStatus);
                    return newScore;
                });

        return Optional.of(rawScoreRepo.save(newRawScore));
    }

    public Optional<RawScore> getRawScoreFromCwidAndAssignmentId(String cwid, UUID assignmentId){
        Optional<RawScore> score = rawScoreRepo.getByCwidAndAssignmentId(cwid, assignmentId);
        if(score.isEmpty()){
            log.warn("Could not find raw score for cwid {} on assignment {}", cwid, assignmentId);
            return Optional.empty();
        }
        return score;
    }

}
