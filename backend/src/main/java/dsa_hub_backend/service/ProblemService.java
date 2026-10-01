package dsa_hub_backend.service;

import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.entity.ProblemLink;
import dsa_hub_backend.entity.ProblemTopic;
import dsa_hub_backend.entity.Sheet;
import dsa_hub_backend.repository.ProblemLinkRepository;
import dsa_hub_backend.repository.ProblemRepository;
import dsa_hub_backend.repository.ProblemTopicRepository;
import dsa_hub_backend.repository.SheetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemLinkRepository problemLinkRepository;
    private final ProblemTopicRepository problemTopicRepository;
    private final SheetRepository sheetRepository;

    public ProblemService(ProblemRepository problemRepository,
                          ProblemLinkRepository problemLinkRepository,
                          ProblemTopicRepository problemTopicRepository,
                          SheetRepository sheetRepository) {
        this.problemRepository = problemRepository;
        this.problemLinkRepository = problemLinkRepository;
        this.problemTopicRepository = problemTopicRepository;
        this.sheetRepository = sheetRepository;
    }

    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    public Problem getProblemById(String problemId) {
        return findProblem(problemId);
    }

    public List<Sheet> getEnabledSheetsForProblem(String problemId) {
        findProblem(problemId);
        return sheetRepository.findEnabledByProblemIdOrderByDisplayOrder(problemId);
    }

    public List<ProblemLink> getProblemLinks(String problemId) {
        findProblem(problemId);
        return problemLinkRepository.findByProblemIdOrderByLinkTypeAsc(problemId);
    }

    public List<ProblemTopic> getProblemTopics(String problemId) {
        findProblem(problemId);
        return problemTopicRepository.findByProblemIdOrderByTopicAsc(problemId);
    }

    private Problem findProblem(String problemId) {
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found"));
    }
}
