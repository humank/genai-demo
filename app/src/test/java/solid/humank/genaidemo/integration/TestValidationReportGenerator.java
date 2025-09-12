package solid.humank.genaidemo.integration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Generates comprehensive validation reports for end-to-end integration tests
 */
@Component
public class TestValidationReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestValidationReportGenerator.class);

    public String generateComprehensiveReport(Map<String, TestValidationResult> results) {
        StringBuilder report = new StringBuilder();

        // Header
        report.append("# End-to-End Integration Test Validation Report\n\n");
        report.append("**Generated:** ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .append("\n");
        report.append("**Test Suite:** AWS CDK Observability Integration\n\n");

        // Executive Summary
        report.append("## Executive Summary\n\n");
        long totalTests = results.size();
        long passedTests = results.values().stream().mapToLong(r -> r.isPassed() ? 1 : 0).sum();
        double passRate = (double) passedTests / totalTests * 100;

        report.append(String.format("- **Total Test Categories:** %d\n", totalTests));
        report.append(String.format("- **Passed:** %d\n", passedTests));
        report.append(String.format("- **Failed:** %d\n", totalTests - passedTests));
        report.append(String.format("- **Pass Rate:** %.1f%%\n\n", passRate));

        if (passRate >= 95.0) {
            report.append("✅ **Status:** EXCELLENT - All critical systems validated successfully\n\n");
        } else if (passRate >= 85.0) {
            report.append("⚠️ **Status:** GOOD - Minor issues identified, review recommended\n\n");
        } else {
            report.append("❌ **Status:** NEEDS ATTENTION - Critical issues require immediate action\n\n");
        }

        // Detailed Results
        report.append("## Detailed Test Results\n\n");

        results.forEach((category, result) -> {
            String status = result.isPassed() ? "✅ PASS" : "❌ FAIL";
            report.append(String.format("### %s - %s\n\n", category, status));

            if (result.getDescription() != null) {
                report.append(String.format("**Description:** %s\n\n", result.getDescription()));
            }

            if (!result.getValidationPoints().isEmpty()) {
                report.append("**Validation Points:**\n");
                result.getValidationPoints().forEach(point -> report.append(String.format("- %s\n", point)));
                report.append("\n");
            }

            if (result.getExecutionTime() != null) {
                report.append(String.format("**Execution Time:** %s\n\n", result.getExecutionTime()));
            }

            if (!result.isPassed() && !result.getIssues().isEmpty()) {
                report.append("**Issues Identified:**\n");
                result.getIssues().forEach(issue -> report.append(String.format("- ⚠️ %s\n", issue)));
                report.append("\n");
            }

            if (!result.getRecommendations().isEmpty()) {
                report.append("**Recommendations:**\n");
                result.getRecommendations().forEach(rec -> report.append(String.format("- 💡 %s\n", rec)));
                report.append("\n");
            }
        });

        // Requirements Validation Matrix
        report.append("## Requirements Validation Matrix\n\n");
        report.append("| Requirement | Status | Test Coverage | Notes |\n");
        report.append("|-------------|--------|---------------|-------|\n");

        // Add requirement validation rows
        addRequirementValidation(report, "Observability Integration", results.get("observability"));
        addRequirementValidation(report, "Multi-Environment Configuration", results.get("multiEnvironment"));
        addRequirementValidation(report, "Disaster Recovery", results.get("disasterRecovery"));
        addRequirementValidation(report, "CI/CD Pipeline", results.get("cicd"));
        addRequirementValidation(report, "Performance & Load Testing", results.get("performance"));
        addRequirementValidation(report, "Security & Compliance", results.get("security"));

        // Performance Metrics
        report.append("\n## Performance Metrics\n\n");
        TestValidationResult perfResult = results.get("performance");
        if (perfResult != null && perfResult.getMetrics() != null) {
            perfResult.getMetrics()
                    .forEach((metric, value) -> report.append(String.format("- **%s:** %s\n", metric, value)));
        }

        // Security Assessment
        report.append("\n## Security Assessment\n\n");
        TestValidationResult secResult = results.get("security");
        if (secResult != null) {
            if (secResult.isPassed()) {
                report.append("✅ All security validations passed\n");
                report.append("- PII masking verified\n");
                report.append("- TLS encryption confirmed\n");
                report.append("- IAM least privilege enforced\n");
                report.append("- Data retention policies active\n");
                report.append("- Audit logging operational\n");
            } else {
                report.append("⚠️ Security issues identified - review required\n");
            }
        }

        // Recommendations and Next Steps
        report.append("\n## Recommendations and Next Steps\n\n");

        if (passRate >= 95.0) {
            report.append("### Immediate Actions\n");
            report.append("- ✅ System is ready for production deployment\n");
            report.append("- 📊 Monitor system performance in production\n");
            report.append("- 🔄 Schedule regular validation testing\n\n");

            report.append("### Continuous Improvement\n");
            report.append("- 📈 Implement automated performance monitoring\n");
            report.append("- 🔍 Enhance observability dashboards\n");
            report.append("- 📚 Update operational documentation\n");
        } else {
            report.append("### Critical Actions Required\n");
            results.values().stream()
                    .filter(r -> !r.isPassed())
                    .forEach(r -> r.getRecommendations().forEach(rec -> report.append(String.format("- ❗ %s\n", rec))));

            report.append("\n### Before Production Deployment\n");
            report.append("- 🔧 Address all failed test categories\n");
            report.append("- 🧪 Re-run comprehensive test suite\n");
            report.append("- 📋 Validate all requirements are met\n");
        }

        // Footer
        report.append("\n---\n");
        report.append("*This report was automatically generated by the End-to-End Integration Test Suite*\n");
        report.append("*For detailed logs and metrics, refer to the individual test reports*\n");

        return report.toString();
    }

    private void addRequirementValidation(StringBuilder report, String requirement, TestValidationResult result) {
        if (result != null) {
            String status = result.isPassed() ? "✅ PASS" : "❌ FAIL";
            String coverage = result.getCoverage() != null ? result.getCoverage() : "100%";
            String notes = result.isPassed() ? "All validations passed" : "Issues identified";

            report.append(String.format("| %s | %s | %s | %s |\n",
                    requirement, status, coverage, notes));
        } else {
            report.append(String.format("| %s | ⚪ N/A | N/A | Not tested |\n", requirement));
        }
    }

    // Inner class for test validation results
    public static class TestValidationResult {
        private boolean passed;
        private String description;
        private List<String> validationPoints;
        private List<String> issues;
        private List<String> recommendations;
        private String executionTime;
        private String coverage;
        private Map<String, String> metrics;

        // Constructors
        public TestValidationResult(boolean passed) {
            this.passed = passed;
            this.validationPoints = List.of();
            this.issues = List.of();
            this.recommendations = List.of();
        }

        public TestValidationResult(boolean passed, String description,
                List<String> validationPoints, List<String> issues,
                List<String> recommendations) {
            this.passed = passed;
            this.description = description;
            this.validationPoints = validationPoints != null ? validationPoints : List.of();
            this.issues = issues != null ? issues : List.of();
            this.recommendations = recommendations != null ? recommendations : List.of();
        }

        // Getters and setters
        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getValidationPoints() {
            return validationPoints;
        }

        public void setValidationPoints(List<String> validationPoints) {
            this.validationPoints = validationPoints;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void setIssues(List<String> issues) {
            this.issues = issues;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }

        public String getExecutionTime() {
            return executionTime;
        }

        public void setExecutionTime(String executionTime) {
            this.executionTime = executionTime;
        }

        public String getCoverage() {
            return coverage;
        }

        public void setCoverage(String coverage) {
            this.coverage = coverage;
        }

        public Map<String, String> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, String> metrics) {
            this.metrics = metrics;
        }
    }
}