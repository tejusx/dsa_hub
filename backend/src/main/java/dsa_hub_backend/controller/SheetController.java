package dsa_hub_backend.controller;

import dsa_hub_backend.entity.Sheet;
import dsa_hub_backend.entity.Problem;
import dsa_hub_backend.service.SheetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sheets")
public class SheetController {

    private final SheetService sheetService;

    public SheetController(SheetService sheetService) {
        this.sheetService = sheetService;
    }

    @GetMapping
    public List<Sheet> getEnabledSheets() {
        return sheetService.getEnabledSheets();
    }

    @GetMapping("/{sheetId}")
    public Sheet getSheetById(@PathVariable String sheetId) {
        return sheetService.getSheetById(sheetId);
    }

    @GetMapping("/{sheetId}/problems")
    public List<Problem> getProblemsForSheet(@PathVariable String sheetId) {
        return sheetService.getProblemsForSheet(sheetId);
    }
}
