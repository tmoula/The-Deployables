package com.outreach.lead.application;



@Service
public class MatchService {
    private SellerProfile seller;                       // set via PUT /seller
    private final List<Prospect> prospects = new ArrayList<>(); // seeded DB

    // 1) Save & get seller
    public SellerProfile getSeller(){ return seller; }
    public SellerProfile setSeller(SellerProfile s){ this.seller = s; return s; }

    // 2) DB listing/adding (admin/debug; UI won’t input names)
    public List<Prospect> listProspects(){ return prospects; }
    public Prospect createProspect(Prospect in){ ... }

    // 3) Match using criteria (user never types names)
    public List<ScoredProspect> match(ProspectCriteria c, int limit){
        if (seller == null) return List.of();

        // (a) FILTER by criteria
        var filtered = prospects.stream().filter(p -> {
            if (c.industry()!=null && p.industry()!=null &&
                    !p.industry().equalsIgnoreCase(c.industry())) return false;

            if (c.minSize()!=null && (p.size()==null || p.size()<c.minSize())) return false;
            if (c.maxSize()!=null && (p.size()==null || p.size()>c.maxSize())) return false;

            if (c.regions()!=null && !c.regions().isEmpty()) {
                if (p.regions()==null || p.regions().stream().noneMatch(c.regions()::contains)) return false;
            }
            if (c.requiredStack()!=null && !c.requiredStack().isEmpty()) {
                if (p.stack()==null || !p.stack().containsAll(c.requiredStack())) return false;
            }
            // keywords are "desired", not required → don’t filter here
            return true;
        });

        // (b) SCORE the filtered set using seller + criteria
        return filtered
                .map(p -> new ScoredProspect(p, score(seller, c, p)))
                .sorted(Comparator.comparingDouble(ScoredProspect::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    // simple, explainable scoring 0..100 (weights are tunable)
    private double score(SellerProfile s, ProspectCriteria c, Prospect p){
        double total = 0;

        // Industry alignment (from seller, if present)
        if (s.industry()!=null && p.industry()!=null &&
                s.industry().equalsIgnoreCase(p.industry())) total += 30;

        // Region overlap (seller target regions vs prospect)
        if (s.regions()!=null && p.regions()!=null) {
            long overlap = p.regions().stream().filter(s.regions()::contains).count();
            if (overlap > 0) total += 15;
        }

        // Size proximity to seller’s size (closer is better)
        if (s.companySize()!=null && p.size()!=null) {
            int diff = Math.abs(s.companySize() - p.size());
            total += Math.max(0, 15 - (diff / 300.0)); // within ~4500 employees still gets some points
        }

        // Criteria-desired keywords (nice-to-have)
        var desiredKw = c.desiredKeywords()==null ? List.<String>of() : c.desiredKeywords();
        var prospectTerms = new ArrayList<String>();
        if (p.keywords()!=null) prospectTerms.addAll(p.keywords());
        if (p.stack()!=null) prospectTerms.addAll(p.stack());
        long kwOverlap = prospectTerms.stream().map(String::toLowerCase)
                .filter(t -> desiredKw.stream().map(String::toLowerCase).anyMatch(t::contains))
                .count();
        total += Math.min(20, kwOverlap * 10.0);

        // Seller value-prop vs prospect keywords/stack
        var vp = s.valuePropositionKeywords()==null ? List.<String>of() : s.valuePropositionKeywords();
        long vpOverlap = prospectTerms.stream().map(String::toLowerCase)
                .filter(t -> vp.stream().map(String::toLowerCase).anyMatch(t::contains))
                .count();
        total += Math.min(20, vpOverlap * 10.0);

        return Math.min(100, total);
    }

    public record ScoredProspect(Prospect prospect, double score) {}
}
