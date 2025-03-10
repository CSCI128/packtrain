package edu.mines.gradingadmin.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    public List<RawScore> uploadCSV(MultipartFile file, UUID migrationId) throws Exception {
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
                    try{
                        String cwid = line[CWID_IDX];

                        // TODO: Need to follow the example from CourseRole here
                        String status = line[STATUS_IDX].trim().toUpperCase();
                        SubmissionStatus submissionStatus;
                        submissionStatus = SubmissionStatus.valueOf(status);

                        // TODO: Less checking for null, and work based on submission status
                        double score = 0.0;
                        if(Double.parseDouble(line[SCORE_IDX]) != 0.0){
                            score = Double.parseDouble(line[SCORE_IDX]);
                        }

                        Instant submissionTime = null;
                        if(!line[SUBMISSION_TIME_IDX].isEmpty()) {
                            submissionTime = LocalDateTime.parse(
                                    line[SUBMISSION_TIME_IDX],
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                            ).atZone(
                                    ZoneId.of("America/Denver")
                            ).toInstant();
                        }

                        double hoursLate = 0.0;
                        if(!line[HOURS_LATE_IDX].isEmpty()){
                            // TODO: Convert from time to hours
                        }



                        RawScore rawScore = createOrUpdateRawScore(migrationId, cwid, score, submissionTime, hoursLate, submissionStatus);
                        scores.add(rawScore);

                    }
                    catch (Exception e){
                        log.warn(e.getMessage());
                        log.warn("Wrong input format for the csv");
                    }

                }
            }

        return scores;
    }

    public RawScore createOrUpdateRawScore(UUID migrationId, String cwid, double score, Instant submissionTime, double hoursLate, SubmissionStatus submissionStatus){
        Optional<RawScore> newRawScore = rawScoreRepo.getByCwidAndMigrationId(cwid, migrationId);
        RawScore rawScore;
        if(newRawScore.isEmpty()){
            rawScore = newRawScore(migrationId, cwid, score, submissionTime, hoursLate, submissionStatus);
        }
        else{
            rawScore = updateRawScore(newRawScore.get(), score, submissionTime, hoursLate, submissionStatus);
        }

        return rawScoreRepo.save(rawScore);
    }

    private RawScore updateRawScore(RawScore rawScore, double score, Instant submissionTime, double hoursLate, SubmissionStatus submissionStatus){
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

    private RawScore newRawScore(UUID migrationId, String cwid, double score, Instant submissionTime, double hoursLate, SubmissionStatus submissionStatus){
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
