package dsa_hub_backend.service;

import dsa_hub_backend.entity.Sheet;
import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.repository.SheetRepository;
import dsa_hub_backend.repository.SheetProblemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SheetService {

    private final SheetRepository sheetRepository;
    private final SheetProblemRepository sheetProblemRepository;

    public SheetService(SheetRepository sheetRepository, SheetProblemRepository sheetProblemRepository) {
        this.sheetRepository = sheetRepository;
        this.sheetProblemRepository = sheetProblemRepository;
    }

    public List<Sheet> getEnabledSheets() {
        return sheetRepository.findByEnabledTrueOrderByDisplayOrderAscSheetIdAsc();
    }

    public Sheet getSheetById(String sheetId) {
        return sheetRepository.findById(sheetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet not found"));
    }

    public List<Problem> getProblemsForSheet(String sheetId) {
        getSheetById(sheetId);
        return sheetProblemRepository.findActiveProblemsBySheetIdOrderByOccurrencePosition(sheetId);
    }
}
