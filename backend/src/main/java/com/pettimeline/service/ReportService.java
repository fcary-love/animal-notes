package com.pettimeline.service;

import com.pettimeline.model.vo.EnhancedReportVO;

public interface ReportService {
    EnhancedReportVO generateEnhancedReport(Long userId, Long petId);
}
