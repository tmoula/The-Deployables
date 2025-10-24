package com.outreach.lead;
import java.util.List;

public record Lead(
  String id, String company, String domain, String role, String name,
  String email, Integer size, String region, List<String> stack, Double score
) {}

