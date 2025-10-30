// Exposes REST endpoints (/api/v1/...) so the frontend can talk to the backend
package com.outreach.lead.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class MatchController {
    private final MatchService svc;
    public MatchController(MatchService svc){ this.svc = svc; }

    @PutMapping("/seller")
    public SellerProfile putSeller(@RequestBody SellerProfile s){ return svc.setSeller(s); }

    @GetMapping("/prospects") public List<Prospect> listProspects(){ return svc.listProspects(); }

    @PostMapping("/match")
    public List<MatchService.ScoredProspect> match(
            @RequestBody(required=false) ProspectCriteria criteria,
            @RequestParam(defaultValue="20") int limit) {
        return svc.match(criteria==null ? new ProspectCriteria(null,null,null,null,null,null) : criteria, limit);
    }

    @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","ok"); }
}
