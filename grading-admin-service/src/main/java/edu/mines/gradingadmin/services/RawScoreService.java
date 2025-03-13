package edu.mines.gradingadmin.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import edu.ksu.canvas.model.Enrollment;
import edu.mines.gradingadmin.models.CourseRole;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.SubstringIndexBottom;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class RawScoreService {

    private static final int CWID_IDX = 2;
    private static final int SCORE_IDX = 6;
    private static final int STATUS_IDX = 8;
    private static final int SUBMISSION_TIME_IDX = 10;
    private static final int HOURS_LATE_IDX = 11;

    private final RawScoreRepo rawScoreRepo;

    public RawScoreService(RawScoreRepo rawScoreRepo){
        this.rawScoreRepo = rawScoreRepo;
    }

    public List<RawScore> uploadCSV(MultipartFile file, UUID migrationId) {
        List<RawScore> scores = new LinkedList<>();

        if (file.isEmpty()){
            log.warn("Provided file {} is empty", file.getName());
            return scores;
        }
        if(file.getContentType() == null){
            log.warn("File content type not defined for file {}", file.getName());
            return scores;
        }
        if (!file.getContentType().equals("text/csv")){
            log.warn("File MIME type is not test/csv for the file {}", file.getName());
            return scores;
        }

        try (InputStream inputStream = file.getInputStream();
             CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(inputStream))
                .withSkipLines(1)
                .build()) {

            String[] line;
            while((line = csvReader.readNext()) != null){
                RawScore rawScore = parseLineToRawScore(migrationId, line);
                scores.add(rawScore);
            }
        }
        catch (Exception e){
            log.warn(e.getMessage());
        }

        return scores;
    }

    private RawScore parseLineToRawScore(UUID migrationId, String[] line){

        String cwid = line[CWID_IDX];

        String status = line[STATUS_IDX].trim();
        SubmissionStatus submissionStatus = mapSubmissionStatus(status);

        if(submissionStatus == SubmissionStatus.MISSING){
            return createOrUpdateRawScore(migrationId, cwid, null, null, null, submissionStatus);
        }else{
            double score = Double.parseDouble(line[SCORE_IDX]);

            Instant submissionTime = LocalDateTime.parse(
                    line[SUBMISSION_TIME_IDX],
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US)
            ).atZone(
                    ZoneId.of("America/Denver")
            ).toInstant();

            double hoursLate = 0.0;
            String[] lateTime = line[HOURS_LATE_IDX].split(":");
            hoursLate += Double.parseDouble(lateTime[0]);
            hoursLate += Double.parseDouble(lateTime[1])/60;
            hoursLate += Double.parseDouble(lateTime[2])/3600;

            return createOrUpdateRawScore(migrationId, cwid, score, submissionTime, hoursLate, submissionStatus);
        }

    }

    public SubmissionStatus mapSubmissionStatus(String status){
        return switch (status) {
            case "Graded" -> SubmissionStatus.GRADED;
            case "Ungraded" -> SubmissionStatus.UNGRADED;
            case "Missing" -> SubmissionStatus.MISSING;
            default -> SubmissionStatus.UNKNOWN;
        };
    }

    public RawScore createOrUpdateRawScore(UUID migrationId, String cwid, Double score, Instant submissionTime, Double hoursLate, SubmissionStatus submissionStatus){
        RawScore rawScore;
        if(rawScoreRepo.existsByCwidAndMigrationId(cwid, migrationId)){
            Optional<RawScore> oldRawScore = rawScoreRepo.getByCwidAndMigrationId(cwid, migrationId);
            System.out.println(oldRawScore);
            rawScore = updateRawScore(oldRawScore.get(), score, submissionTime, hoursLate, submissionStatus);
        }else{
            rawScore = createRawScore(migrationId, cwid, score, submissionTime, hoursLate, submissionStatus);
        }
        return rawScoreRepo.save(rawScore);
    }

    private RawScore updateRawScore(RawScore rawScore, Double score, Instant submissionTime, Double hoursLate, SubmissionStatus submissionStatus){
        UUID migrationId = rawScore.getMigrationId();
        String cwid = rawScore.getCwid();

        log.warn("Overwriting raw score for migration {} with user {}", migrationId, cwid);
        rawScore.setSubmissionStatus(submissionStatus);
        rawScore.setMigrationId(migrationId);
        rawScore.setCwid(cwid);
        rawScore.setScore(score);
        rawScore.setSubmissionTime(submissionTime);
        rawScore.setHoursLate(hoursLate);
        rawScore.setSubmissionStatus(submissionStatus);
        return rawScore;
    }

    private RawScore createRawScore(UUID migrationId, String cwid, Double score, Instant submissionTime, Double hoursLate, SubmissionStatus submissionStatus){
        RawScore newScore = new RawScore();
        newScore.setMigrationId(migrationId);
        newScore.setCwid(cwid);
        newScore.setScore(score);
        newScore.setSubmissionTime(submissionTime);
        newScore.setHoursLate(hoursLate);
        newScore.setSubmissionStatus(submissionStatus);
        return newScore;
    }

    public Optional<RawScore> getRawScoreFromCwidAndAssignmentId(String cwid, UUID migrationId){
        Optional<RawScore> score = rawScoreRepo.getByCwidAndMigrationId(cwid, migrationId);
        if(score.isEmpty()){
            log.warn("Could not find raw score for cwid {} on migration id {}", cwid, migrationId);
            return Optional.empty();
        }
        return score;
    }

}
