package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.UserCategorieScoreAdminDto;
import com.tinyspring.garderie.service.boutique.AffiniteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/boutique")
@PreAuthorize("hasRole('ADMIN')")
public class AffiniteAdminController {

    private final AffiniteService affiniteService;

    public AffiniteAdminController(AffiniteService affiniteService) {
        this.affiniteService = affiniteService;
    }

    // GET /api/admin/boutique/affinites
    @GetMapping("/affinites")
    public ResponseEntity<List<UserCategorieScoreAdminDto>> getAllAffinites() {
        return ResponseEntity.ok(affiniteService.getAllScoresAdmin());
    }

    // GET /api/admin/boutique/affinites/user/{userId}
    @GetMapping("/affinites/user/{userId}")
    public ResponseEntity<List<UserCategorieScoreAdminDto>> getAffinitesByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(affiniteService.getScoresByUserAdmin(userId));
    }
}