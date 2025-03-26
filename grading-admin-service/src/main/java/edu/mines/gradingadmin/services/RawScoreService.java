package edu.mines.gradingadmin.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
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

    public List<RawScore> uploadCSV(InputStream file, UUID migrationId) {
        List<RawScore> scores = new LinkedList<>();

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .withSkipLines(1)
                .build()) {

            String[] line;
            while((line = csvReader.readNext()) != null){
                RawScore rawScore = parseLineToRawScore(migrationId, line);
                scores.add(rawScore);
            }
        }
        catch (Exception e){
            log.error("Failed to read CSV", e);
        }

        return scores;
    }

    private RawScore parseLineToRawScore(UUID migrationId, String[] line){

        String cwid = line[CWID_IDX];

        String status = line[STATUS_IDX].trim();

        if(status.equals("Missing"))
            return createOrUpdateRawScore(migrationId, cwid, null, null, null, SubmissionStatus.MISSING);

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

        SubmissionStatus submissionStatus = SubmissionStatus.ON_TIME;

        if(hoursLate > 0)
            submissionStatus = SubmissionStatus.LATE;

        return createOrUpdateRawScore(migrationId, cwid, score, submissionTime, hoursLate, submissionStatus);

    }

    public RawScore createOrUpdateRawScore(UUID migrationId, String cwid, Double score, Instant submissionTime, Double hoursLate, SubmissionStatus submissionStatus){
        RawScore rawScore;
        if(rawScoreRepo.existsByCwidAndMigrationId(cwid, migrationId)){
            Optional<RawScore> oldRawScore = rawScoreRepo.getByCwidAndMigrationId(cwid, migrationId);
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

    public Optional<RawScore> getRawScoreForCwidAndMigrationId(String cwid, UUID migrationId){
        Optional<RawScore> score = rawScoreRepo.getByCwidAndMigrationId(cwid, migrationId);
        if(score.isEmpty()){
            log.warn("Could not find raw score for cwid {} on migration id {}", cwid, migrationId);
            return Optional.empty();
        }
        return score;
    }

    public List<RawScore> getRawScoresFromMigration(UUID migrationId){
        return rawScoreRepo.getByMigrationId(migrationId);
    }

}
