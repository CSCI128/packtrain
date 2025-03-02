package edu.mines.gradingadmin.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RawScoreService {

    private final RawScoreRepo rawScoreRepo;

    public RawScoreService(RawScoreRepo rawScoreRepo){
        this.rawScoreRepo = rawScoreRepo;
    }

    public List<RawScore> uploadRawScores(Path filePath) throws Exception {
        List<RawScore> scores = new LinkedList<>();

        if (!Files.exists(filePath)){
            log.warn("Failed to find the file {}", filePath.toString());
            return scores;
        }
        if (!FilenameUtils.getExtension(filePath.toString()).equals("csv")){
            log.warn("File extension is not .csv for the file {}", filePath.toString());
            return scores;
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            try(CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withSkipLines(1)
                    .build()){
                String[] line;
                while((line = csvReader.readNext()) != null){
                    continue;
                }
            }
        }

        return scores;
    }

    public Optional<RawScore> createRawScore(UUID migrationId, UUID assignmentId, String cwid, SubmissionStatus submissionStatus){
        RawScore newRawScore = rawScoreRepo.getByCwidandAssignmentId(cwid, assignmentId)
                .map(score -> {
                    log.warn("Overwriting raw score for {} with user {}", score.getAssignment_id(), score.getCwid());
                    score.setSubmissionStatus(submissionStatus);
                    score.setMigration_id(migrationId);
                    return score;
                })
                .orElseGet(() -> {
                    RawScore newScore = new RawScore();
                    newScore.setMigration_id(migrationId);
                    newScore.setAssignment_id(assignmentId);
                    newScore.setCwid(cwid);
                    newScore.setSubmissionStatus(submissionStatus);
                    return newScore;
                });

        return Optional.of(rawScoreRepo.save(newRawScore));
    }

}
