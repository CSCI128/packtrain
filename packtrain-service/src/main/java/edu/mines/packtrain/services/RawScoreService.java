package edu.mines.packtrain.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import edu.mines.packtrain.models.Assignment;
import edu.mines.packtrain.models.Course;
import edu.mines.packtrain.models.RawScore;
import edu.mines.packtrain.models.enums.ExternalAssignmentType;
import edu.mines.packtrain.models.enums.SubmissionStatus;
import edu.mines.packtrain.repositories.RawScoreRepo;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class RawScoreService {
    private final String timeZone;
    private final RawScoreRepo rawScoreRepo;
    private final CourseMemberService courseMemberService;
    private final MigrationService migrationService;

    public RawScoreService(
            @Value("${grading-admin.time-zone}") String timeZone,
            RawScoreRepo rawScoreRepo, CourseMemberService courseMemberService,
            MigrationService migrationService) {
        this.timeZone = timeZone;
        this.rawScoreRepo = rawScoreRepo;
        this.courseMemberService = courseMemberService;
        this.migrationService = migrationService;
    }

    public void uploadGradescopeCSV(InputStream file, UUID migrationId) {
        List<RawScore> scores = List.of();

        if (!migrationService.attemptToStartRawScoreImport(migrationId, "Import of " +
                "Gradescope scores started.", ExternalAssignmentType.GRADESCOPE)) {
            log.error("Failed to start raw score import for GS!");
            return;
        }

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .withSkipLines(1)
                .build()) {

            scores = csvReader.readAll().stream().map(l -> parseLineGS(migrationId, l))
                    .filter(Optional::isPresent).map(Optional::get).toList();
        } catch (Exception e) {
            log.error("Failed to read CSV", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (!migrationService.finishRawScoreImport(migrationId, String.format("%s raw scores " +
                "were imported!", scores.size()))) {
            log.error("Failed to complete raw score import for GS!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to " +
                    "complete raw score import for GS!");
        }
    }

    public void uploadPrairieLearnCSV(InputStream file, UUID migrationId) {
        List<RawScore> scores = List.of();

        if (!migrationService.attemptToStartRawScoreImport(migrationId, "Import of " +
                "PrairieLearn scores started.", ExternalAssignmentType.PRAIRIELEARN)) {
            log.error("Failed to start raw score import for PL!");
            return;
        }

        boolean groupMode = false;

        Course course = migrationService.getCourseForMigration(migrationId);
        Assignment assignment = migrationService.getAssignmentForMigration(migrationId);

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .build()
        ) {

            String[] s = csvReader.readNext();

            if (s == null) {
                return;
            }

            List<String> header = Arrays.stream(s).toList();

            if (header.contains("Group name")) {
                groupMode = true;
            }

            List<String[]> contents = groupMode ? convertGroupSubmissionToIndividualSubmission(
                    header, csvReader.readAll()) : reduceColumns(header, csvReader.readAll());

            scores = contents.stream().map(l ->
                            parseLinePL(course, assignment, migrationId, l))
                    .filter(Optional::isPresent).map(Optional::get).toList();

        } catch (IOException e) {
            log.error("Failed to read CSV!", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (CsvException e) {
            log.error("Failed to parse CSV line!", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        if (!migrationService.finishRawScoreImport(migrationId, String.format("%s raw scores were" +
                " imported!", scores.size()))) {
            log.error("Failed to complete raw score import for PL!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to " +
                    "complete raw score import for GS!");
        }
    }

    public void uploadRunestoneCSV(InputStream file, UUID migrationId) {
        List<RawScore> scores = List.of();

        if (!migrationService.attemptToStartRawScoreImport(migrationId, "Import of " +
                "Runestone scores started.", ExternalAssignmentType.RUNESTONE)) {
            log.error("Failed to start raw score import for Runestone!");
            return;
        }

        Course course = migrationService.getCourseForMigration(migrationId);
        Assignment assignment = migrationService.getAssignmentForMigration(migrationId);

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .build()) {
            String[] headerLine = csvReader.readNext();
            if (headerLine == null) {
                log.error("Missing header for Runestone import!");
                return;
            }

            int assignmentIdx = -1;
            // find assignment column because Runestone doesn't put them in order
            for (int i = 0; i < headerLine.length; i++) {
                if (headerLine[i].equalsIgnoreCase(assignment.getName())) {
                    assignmentIdx = i;
                }
            }

            int finalAssignmentIdx = assignmentIdx; // lambda vars have to be final
            scores = csvReader.readAll().stream().map(l ->
                            parseLineRunestone(finalAssignmentIdx, course, assignment,
                                    migrationId, l))
                    .filter(Optional::isPresent).map(Optional::get).toList();
        } catch (Exception e) {
            log.error("Failed to read CSV", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());

        }

        if (!migrationService.finishRawScoreImport(migrationId, String.format("%s raw scores " +
                "were imported!", scores.size()))) {
            log.error("Failed to complete raw score import for Runestone!");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to " +
                    "complete raw score import for GS!");
        }

    }

    private String[] extractMembersFromGroup(String groupMembers) {
        groupMembers = groupMembers.replaceAll("[\\[\"\\]]", "");

        return groupMembers.split(",");
    }


    private List<String[]> convertGroupSubmissionToIndividualSubmission(List<String> header,
                                                                        List<String[]> csv) {
        List<String[]> normalizeGroupSubmissions = new LinkedList<>();

        final int GROUP_MEMBERS_IDX = header.indexOf("Usernames");
        final int SUBMISSION_DATE_IDX = header.indexOf("Submission date");
        final int QUESTION_POINTS_IDX = header.indexOf("Question points");

        for (String[] line : csv) {
            String[] members = extractMembersFromGroup(line[GROUP_MEMBERS_IDX]);
            for (String member : members) {
                normalizeGroupSubmissions.add(new String[]{member.strip(),
                        line[SUBMISSION_DATE_IDX], line[QUESTION_POINTS_IDX]});
            }
        }

        return normalizeGroupSubmissions;
    }

    private List<String[]> reduceColumns(List<String> header, List<String[]> csv) {
        List<String[]> reducedSubmissions = new LinkedList<>();

        final int USER_ID_INDEX = header.indexOf("UID");
        final int SUBMISSION_DATE_IDX = header.indexOf("Submission date");
        final int QUESTION_POINTS_IDX = header.indexOf("Question points");
        for (String[] line : csv) {
            reducedSubmissions.add(new String[]{line[USER_ID_INDEX],
                    line[SUBMISSION_DATE_IDX], line[QUESTION_POINTS_IDX]});
        }

        return reducedSubmissions;
    }

    private Optional<RawScore> parseLinePL(Course course, Assignment assignment, UUID migrationId,
                                           String[] line) {
        final int USER_ID_IDX = 0;
        final int SUBMISSION_DATE_IDX = 1;
        final int POINTS_IDX = 2;

        RawScore s = new RawScore();

        Optional<String> cwid = courseMemberService.getCwidGivenCourseAndEmail(line[USER_ID_IDX],
                course);

        if (cwid.isEmpty()) {
            log.warn("Student '{}' is not a member of '{}'", line[USER_ID_IDX], course.getCode());
            return Optional.empty();
        }

        Instant submissionTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(
                line[SUBMISSION_DATE_IDX], Instant::from);

        SubmissionStatus status = SubmissionStatus.ON_TIME;

        double hoursLate = 0;

        if (assignment.getDueDate() != null) {
            if (assignment.getDueDate().isBefore(submissionTime)) {
                status = SubmissionStatus.LATE;
                Instant late = submissionTime.minusMillis(assignment.getDueDate().toEpochMilli());
                hoursLate = (double) late.getEpochSecond() / (60 * 60);
            }
        }

        double score = Double.parseDouble(line[POINTS_IDX]);

        s.setMigrationId(migrationId);
        s.setCwid(cwid.get());
        s.setHoursLate(hoursLate);
        s.setSubmissionStatus(status);
        s.setSubmissionTime(submissionTime);
        s.setScore(score);

        return Optional.of(createOrIncrementScore(s));
    }

    private Optional<RawScore> parseLineRunestone(int assignmentIdx, Course course,
                                                  Assignment assignment, UUID migrationId,
                                                  String[] line) {
        if (assignmentIdx == -1) {
            log.warn("Could not find the specified assignment in the Runestone CSV!");
            return Optional.empty();
        }

        final int USER_ID_IDX = 2;

        RawScore s = new RawScore();

        Optional<String> cwid = courseMemberService.getCwidGivenCourseAndEmail(
                line[USER_ID_IDX], course);

        if (cwid.isEmpty()) {
            log.warn("Student '{}' is not a member of '{}'", line[USER_ID_IDX], course.getCode());
            return Optional.empty();
        }

        s.setMigrationId(migrationId);
        s.setCwid(cwid.get());
        s.setHoursLate(0.0);
        s.setSubmissionStatus(SubmissionStatus.ON_TIME);
        s.setSubmissionTime(assignment.getDueDate().minus(Duration.ofMinutes(1)));
        // Runestone does not track submission times ^^
        s.setScore(Double.parseDouble(line[assignmentIdx]));

        return Optional.of(createOrIncrementScore(s));
    }

    private RawScore createOrIncrementScore(RawScore incoming) {
        if (!rawScoreRepo.existsByCwidAndMigrationId(incoming.getCwid(),
                incoming.getMigrationId())) {
            return rawScoreRepo.save(incoming);
        }

        RawScore existing = getRawScoreForCwidAndMigration(incoming.getCwid(),
                incoming.getMigrationId()).orElseThrow();

        if (existing.getSubmissionTime().isAfter(incoming.getSubmissionTime())) {
            existing.setSubmissionTime(incoming.getSubmissionTime());
            existing.setSubmissionStatus(incoming.getSubmissionStatus());
            existing.setHoursLate(incoming.getHoursLate());
        }

        existing.setScore(existing.getScore() + incoming.getScore());

        return rawScoreRepo.save(existing);
    }


    private Optional<RawScore> parseLineGS(UUID migrationId, String[] line) {
        final int CWID_IDX = 2;
        final int SCORE_IDX = 5;
        final int STATUS_IDX = 7;
        final int SUBMISSION_TIME_IDX = 9;
        final int HOURS_LATE_IDX = 10;

        String cwid = line[CWID_IDX].trim();

        if (rawScoreRepo.existsByCwidAndMigrationId(cwid, migrationId)) {
            log.warn("Duplicate score for '{}'", cwid);
            return Optional.empty();
        }

        String status = line[STATUS_IDX].trim();

        RawScore newScore = new RawScore();
        newScore.setMigrationId(migrationId);
        newScore.setCwid(cwid);

        if (status.equals("Ungraded")) {
            log.warn("Ungraded submission for '{}' for migration '{}'", cwid, migrationId);
            return Optional.empty();
        }

        if (status.equals("Missing")) {
            newScore.setSubmissionStatus(SubmissionStatus.MISSING);
            return Optional.of(rawScoreRepo.save(newScore));
        }

        double score = Double.parseDouble(line[SCORE_IDX]);

        Instant submissionTime = LocalDateTime.parse(
                        line[SUBMISSION_TIME_IDX],
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US))
                .atZone(ZoneId.of(timeZone)).toInstant();

        double hoursLate = 0.0;
        String[] lateTime = line[HOURS_LATE_IDX].split(":");
        hoursLate += Double.parseDouble(lateTime[0]);
        hoursLate += Double.parseDouble(lateTime[1]) / 60;
        hoursLate += Double.parseDouble(lateTime[2]) / (60 * 60);

        SubmissionStatus submissionStatus = SubmissionStatus.ON_TIME;

        if (hoursLate > 0)
            submissionStatus = SubmissionStatus.LATE;

        newScore.setScore(score);
        newScore.setSubmissionTime(submissionTime);
        newScore.setHoursLate(hoursLate);
        newScore.setSubmissionStatus(submissionStatus);


        return Optional.of(rawScoreRepo.save(newScore));
    }

    public Optional<RawScore> getRawScoreForCwidAndMigration(String cwid, UUID migrationId) {
        Optional<RawScore> score = rawScoreRepo.getByCwidAndMigrationId(cwid, migrationId);
        if (score.isEmpty()) {
            log.warn("Could not find raw score for cwid {} on migration id {}", cwid, migrationId);
            return Optional.empty();
        }
        return score;
    }

    public List<RawScore> getRawScoresFromMigration(UUID migrationId) {
        return rawScoreRepo.getByMigrationId(migrationId);
    }

}