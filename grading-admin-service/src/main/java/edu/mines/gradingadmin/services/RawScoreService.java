package edu.mines.gradingadmin.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import edu.mines.gradingadmin.models.Assignment;
import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.RawScore;
import edu.mines.gradingadmin.models.enums.ExternalAssignmentType;
import edu.mines.gradingadmin.models.enums.SubmissionStatus;
import edu.mines.gradingadmin.repositories.RawScoreRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;

@Service
@Slf4j
public class RawScoreService {
    private final String timeZone;
    private final RawScoreRepo rawScoreRepo;
    private final CourseMemberService courseMemberService;
    private final MigrationService migrationService;

    public RawScoreService(
            @Value("${grading-admin.time-zone}") String timeZone,
            RawScoreRepo rawScoreRepo, CourseMemberService courseMemberService, MigrationService migrationService){
        this.timeZone = timeZone;
        this.rawScoreRepo = rawScoreRepo;
        this.courseMemberService = courseMemberService;
        this.migrationService = migrationService;
    }

    public List<RawScore> uploadGradescopeCSV(InputStream file, UUID migrationId) {
        List<RawScore> scores = List.of();

        if (!migrationService.attemptToStartRawScoreImport(migrationId.toString(), "Import of GradeScope scores started.", ExternalAssignmentType.GRADESCOPE)){
            log.error("Failed to start raw score import for GS!");
            return scores;
        }

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .withSkipLines(1)
                .build()) {

            scores = csvReader.readAll().stream().map(l -> parseLineGS(migrationId, l)).toList();
        }
        catch (Exception e){
            log.error("Failed to read CSV", e);
        }

        if (!migrationService.finishRawScoreImport(migrationId.toString(), String.format("%s raw scores were imported!", scores.size()))){
            log.error("Failed to complete raw score import for GS!");
        }
        return scores;
    }

    public List<RawScore> uploadRunestoneCSV(InputStream file, UUID migrationId) {
        List<RawScore> scores = List.of();

        if (!migrationService.attemptToStartRawScoreImport(migrationId.toString(), "Import of Runestone scores started.", ExternalAssignmentType.RUNESTONE)){
            log.error("Failed to start raw score import for Runestone!");
            return scores;
        }

        Course course = migrationService.getCourseForMigration(migrationId.toString());
        Assignment assignment = migrationService.getAssignmentForMigration(migrationId.toString());

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .build()) {
            // find assignment column because Runestone doesn't put them in order
            String[] headerLine = csvReader.readNext();

            scores = csvReader.readAll().stream().map(l -> parseLineRunestone(headerLine, course, assignment, migrationId, l)).filter(Optional::isPresent).map(Optional::get).toList();
        }
        catch (Exception e){
            log.error("Failed to read CSV", e);
        }

        if (!migrationService.finishRawScoreImport(migrationId.toString(), String.format("%s raw scores were imported!", scores.size()))){
            log.error("Failed to complete raw score import for Runestone!");
        }
        return scores;
    }

    public List<RawScore> uploadPrairieLearnCSV(InputStream file, UUID migrationId){
        List<RawScore> scores = List.of();

        if (!migrationService.attemptToStartRawScoreImport(migrationId.toString(), "Import of PrairieLearn scores started.", ExternalAssignmentType.PRAIRIELEARN)){
            log.error("Failed to start raw score import for PL!");
            return scores;
        }

        boolean groupMode = false;

        Course course = migrationService.getCourseForMigration(migrationId.toString());
        Assignment assignment = migrationService.getAssignmentForMigration(migrationId.toString());

        try (CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(file))
                .build()
        ){

            List<String> header = Arrays.stream(csvReader.readNext()).toList();

            if (header.contains("Group name")){
                groupMode = true;
            }

            List<String[]> contents = groupMode ? convertGroupSubmissionToIndividualSubmission(header, csvReader.readAll()) : reduceColumns(header, csvReader.readAll());

            scores = contents.stream().map(l -> parseLinePL(course, assignment, migrationId, l)).filter(Optional::isPresent).map(Optional::get).toList();

        } catch (IOException e) {
            log.error("Failed to read CSV!", e);
        } catch (CsvException e) {
            log.error("Failed to parse CSV line!", e);
        }

        if (!migrationService.finishRawScoreImport(migrationId.toString(), String.format("%s raw scores were imported!", scores.size()))){
            log.error("Failed to complete raw score import for PL!");
        }

        return scores;
    }

    private String[] extractMembersFromGroup(String groupMembers){

        groupMembers = groupMembers.replaceAll("[\\[\"\\]]", "");

        return groupMembers.split(",");
    }


    private List<String[]> convertGroupSubmissionToIndividualSubmission(List<String> header, List<String[]> csv){
        List<String[]> normalizeGroupSubmissions = new LinkedList<>();

        final int GROUP_MEMBERS_IDX = header.indexOf("Usernames");
        final int SUBMISSION_DATE_IDX = header.indexOf("Submission date");
        final int QUESTION_POINTS_IDX = header.indexOf("Question points");

        for (String[] line : csv){
            String[] members = extractMembersFromGroup(line[GROUP_MEMBERS_IDX]);
            for (String member : members){
                normalizeGroupSubmissions.add(new String[]{member, line[SUBMISSION_DATE_IDX], line[QUESTION_POINTS_IDX]});
            }
        }

        return normalizeGroupSubmissions;
    }

    private List<String[]> reduceColumns(List<String> header, List<String[]> csv){
        List<String[]> reducedSubmissions = new LinkedList<>();

        final int USER_ID_INDEX = header.indexOf("UID");
        final int SUBMISSION_DATE_IDX = header.indexOf("Submission date");
        final int QUESTION_POINTS_IDX = header.indexOf("Question points");
        for (String[] line : csv){
            reducedSubmissions.add(new String[]{line[USER_ID_INDEX], line[SUBMISSION_DATE_IDX], line[QUESTION_POINTS_IDX]});
        }

        return reducedSubmissions;
    }

    private Optional<RawScore> parseLineRunestone(String[] header, Course course, Assignment assignment, UUID migrationId, String[] line){
        final int USER_ID_IDX = 2;
        int ASSIGNMENT_IDX = -1;
        for(int i = 0; i < header.length; i++) {
            if(header[i].equalsIgnoreCase(assignment.getName())) {
                ASSIGNMENT_IDX = i;
            }
        }

        if(ASSIGNMENT_IDX == -1) {
            log.warn("Could not find the specified assignment in the Runestone CSV!");
            return Optional.empty();
        }

        RawScore s = new RawScore();

        Optional<String> cwid = courseMemberService.getCwidGivenCourseAndEmail(line[USER_ID_IDX], course);

        if (cwid.isEmpty()){
            log.warn("Student '{}' is not a member of '{}'", line[USER_ID_IDX], course.getCode());
            return Optional.empty();
        }

        s.setMigrationId(migrationId);
        s.setCwid(cwid.get());
        s.setHoursLate(0.0);
        s.setSubmissionStatus(SubmissionStatus.ON_TIME);
        s.setSubmissionTime(Instant.now()); // Runestone does not track submission times

        double score = Integer.parseInt(line[ASSIGNMENT_IDX]);

        // TODO need max score from Runestone
        final int MAX_SCORE = 24;
        score = (score / MAX_SCORE) * 100;

        // TODO need max attainable score from Canvas (sometimes Rob does 4 or 5); this should also probably be policy.
        // Rounded score: round to the nearest multiple of 12.5%
        s.setScore(Math.ceil(score / 12.5) * 0.125 * 5);

        return Optional.of(createOrIncrementScore(s));
    }

    private Optional<RawScore> parseLinePL(Course course, Assignment assignment, UUID migrationId, String[] line){
        final int USER_ID_IDX = 0;
        final int SUBMISSION_DATE_IDX = 1;
        final int POINTS_IDX = 2;

        RawScore s = new RawScore();

        Optional<String> cwid = courseMemberService.getCwidGivenCourseAndEmail(line[USER_ID_IDX], course);

        if (cwid.isEmpty()){
            log.warn("Student '{}' is not a member of '{}'", line[USER_ID_IDX], course.getCode());
            return Optional.empty();
        }

        Instant submissionTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(line[SUBMISSION_DATE_IDX], Instant::from);

        SubmissionStatus status = SubmissionStatus.ON_TIME;

        double hoursLate = 0;

        if (assignment.getDueDate() != null){
            if (assignment.getDueDate().isBefore(submissionTime)){
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

    private RawScore createOrIncrementScore(RawScore incoming){
        if (!rawScoreRepo.existsByCwidAndMigrationId(incoming.getCwid(), incoming.getMigrationId())){
            return rawScoreRepo.save(incoming);
        }

        RawScore existing = getRawScoreForCwidAndMigrationId(incoming.getCwid(), incoming.getMigrationId()).orElseThrow();

        if (existing.getSubmissionTime().isAfter(incoming.getSubmissionTime())){
            existing.setSubmissionTime(incoming.getSubmissionTime());
            existing.setSubmissionStatus(incoming.getSubmissionStatus());
            existing.setHoursLate(incoming.getHoursLate());
        }

        existing.setScore(existing.getScore() + incoming.getScore());

        return rawScoreRepo.save(existing);
    }


    private RawScore parseLineGS(UUID migrationId, String[] line){
        final int CWID_IDX = 2;
        final int SCORE_IDX = 6;
        final int STATUS_IDX = 8;
        final int SUBMISSION_TIME_IDX = 10;
        final int HOURS_LATE_IDX = 11;

        String cwid = line[CWID_IDX].trim();

        String status = line[STATUS_IDX].trim();

        RawScore newScore = new RawScore();
        newScore.setMigrationId(migrationId);
        newScore.setCwid(cwid);

        if(status.equals("Missing")){
            newScore.setSubmissionStatus(SubmissionStatus.MISSING);
            return rawScoreRepo.save(newScore);
        }

        double score = Double.parseDouble(line[SCORE_IDX]);

        Instant submissionTime = LocalDateTime.parse(
                line[SUBMISSION_TIME_IDX],
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US))
        .atZone(ZoneId.of(timeZone)).toInstant();

        double hoursLate = 0.0;
        String[] lateTime = line[HOURS_LATE_IDX].split(":");
        hoursLate += Double.parseDouble(lateTime[0]);
        hoursLate += Double.parseDouble(lateTime[1])/60;
        hoursLate += Double.parseDouble(lateTime[2])/ (60 * 60);

        SubmissionStatus submissionStatus = SubmissionStatus.ON_TIME;

        if(hoursLate > 0)
            submissionStatus = SubmissionStatus.LATE;

        newScore.setScore(score);
        newScore.setSubmissionTime(submissionTime);
        newScore.setHoursLate(hoursLate);
        newScore.setSubmissionStatus(submissionStatus);

        return rawScoreRepo.save(newScore);
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
