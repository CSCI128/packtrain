package edu.mines.gradingadmin.services;

import edu.mines.gradingadmin.models.Course;
import edu.mines.gradingadmin.models.Section;
import edu.mines.gradingadmin.repositories.SectionRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SectionService {
    private final SectionRepo sectionRepo;
    private final CanvasService canvasService;

    public SectionService(SectionRepo sectionRepo, CanvasService canvasService) {
        this.sectionRepo = sectionRepo;
        this.canvasService = canvasService;
    }

    public Map<String, Section> createSectionsFromCanvas(Course course){
        List<edu.ksu.canvas.model.Section> canvasSections = canvasService.getCourseSections(course.getCanvasId());

        return canvasSections.stream().map(section -> {
            var newSection = new Section();
            newSection.setCanvasId(section.getId().toString());
            newSection.setName(section.getName());
            newSection.setCourse(course);
            return sectionRepo.save(newSection);
        }).collect(Collectors.toMap(Section::getCanvasId, section -> section));
    }
}
