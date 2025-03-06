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

                        System.out.println(Arrays.toString(line));

                        SubmissionStatus submissionStatus;
                        try {
                            submissionStatus = SubmissionStatus.valueOf(status);
                        }
                        catch (IllegalArgumentException e) {
                            log.warn("Invalid submission status {} for cwid {}. Skipping this raw score", status, cwid);
                            continue;
                        }

                        Optional<RawScore> score = createRawScore(migrationID, assignmentId, cwid, submissionStatus);

                        if(score.isEmpty()){
                            log.warn("Could not create raw score for {} on assignment {}", cwid, assignmentId);
                            continue;
                        }

                        scores.add(score.get());

                    }
                    catch (Exception e){
                        log.warn("Wrong input format for the csv");
                    }

                }
            }
        }

        return scores;
    }

    public Optional<RawScore> createRawScore(UUID migrationId, UUID assignmentId, String cwid, SubmissionStatus submissionStatus){
        RawScore newRawScore = rawScoreRepo.getByCwidAndAssignmentId(cwid, assignmentId)
                .map(score -> {
                    log.warn("Overwriting raw score for {} with user {}", score.getAssignmentId(), score.getCwid());
                    score.setSubmissionStatus(submissionStatus);
                    score.setMigrationId(migrationId);
                    return score;
                })
                .orElseGet(() -> {
                    RawScore newScore = new RawScore();
                    newScore.setMigrationId(migrationId);
                    newScore.setAssignmentId(assignmentId);
                    newScore.setCwid(cwid);
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
