package dsa_hub_backend.controller;

import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.entity.ProblemLink;
import dsa_hub_backend.entity.ProblemTopic;
import dsa_hub_backend.entity.Sheet;
import dsa_hub_backend.service.ProblemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public List<Problem> getAllProblems() {
        return problemService.getAllProblems();
    }

    @GetMapping("/{problemId}")
    public Problem getProblemById(@PathVariable String problemId) {
        return problemService.getProblemById(problemId);
    }

    @GetMapping("/{problemId}/sheets")
    public List<Sheet> getEnabledSheetsForProblem(@PathVariable String problemId) {
        return problemService.getEnabledSheetsForProblem(problemId);
    }

    @GetMapping("/{problemId}/links")
    public List<ProblemLink> getProblemLinks(@PathVariable String problemId) {
        return problemService.getProblemLinks(problemId);
    }

    @GetMapping("/{problemId}/topics")
    public List<ProblemTopic> getProblemTopics(@PathVariable String problemId) {
        return problemService.getProblemTopics(problemId);
    }
}
