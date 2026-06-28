package com.sapo.mock.clothing.customer.service;

import com.sapo.mock.clothing.customer.dto.response.AiResultDto;

public interface AiAnalysisService {
    AiResultDto analyzeNote(String note);
}
