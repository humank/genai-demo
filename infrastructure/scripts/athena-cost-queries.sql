-- ============================================
-- AWS Cost and Usage Reports - Athena Queries
-- ============================================
-- 
-- Collection of useful Athena queries for cost analysis
-- Database: cost_usage_reports
-- Table: cost_usage_reports (auto-created by Glue Crawler)
--
-- Requirements: 13.22, 13.23, 13.24
-- ============================================

-- ============================================
-- 1. DAILY COST TREND ANALYSIS
-- ============================================

-- Query: Daily costs for the last 30 days
SELECT 
    DATE(line_item_usage_start_date) as usage_date,
    SUM(line_item_unblended_cost) as daily_cost,
    COUNT(DISTINCT line_item_resource_id) as unique_resources
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -30, current_date)
GROUP BY DATE(line_item_usage_start_date)
ORDER BY usage_date DESC;

-- Query: Week-over-week cost comparison
WITH this_week AS (
    SELECT SUM(line_item_unblended_cost) as cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
),
last_week AS (
    SELECT SUM(line_item_unblended_cost) as cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -14, current_date)
        AND line_item_usage_start_date < date_add('day', -7, current_date)
)
SELECT 
    tw.cost as this_week_cost,
    lw.cost as last_week_cost,
    (tw.cost - lw.cost) as cost_difference,
    ((tw.cost - lw.cost) / lw.cost * 100) as percent_change
FROM this_week tw, last_week lw;

-- ============================================
-- 2. SERVICE-LEVEL COST BREAKDOWN
-- ============================================

-- Query: Top 10 services by cost (last 7 days)
SELECT 
    product_servicename,
    SUM(line_item_unblended_cost) as service_cost,
    COUNT(DISTINCT line_item_resource_id) as resource_count,
    AVG(line_item_unblended_cost) as avg_cost_per_resource
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND product_servicename IS NOT NULL
GROUP BY product_servicename
ORDER BY service_cost DESC
LIMIT 10;

-- Query: Service cost trend (daily breakdown for top 5 services)
WITH top_services AS (
    SELECT product_servicename
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    GROUP BY product_servicename
    ORDER BY SUM(line_item_unblended_cost) DESC
    LIMIT 5
)
SELECT 
    DATE(line_item_usage_start_date) as usage_date,
    product_servicename,
    SUM(line_item_unblended_cost) as daily_service_cost
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -30, current_date)
    AND product_servicename IN (SELECT product_servicename FROM top_services)
GROUP BY DATE(line_item_usage_start_date), product_servicename
ORDER BY usage_date DESC, daily_service_cost DESC;

-- ============================================
-- 3. RESOURCE-LEVEL COST ATTRIBUTION
-- ============================================

-- Query: Most expensive resources (last 7 days)
SELECT 
    line_item_resource_id,
    product_servicename,
    SUM(line_item_unblended_cost) as resource_cost,
    line_item_usage_type,
    line_item_operation,
    product_region
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND line_item_resource_id IS NOT NULL
    AND line_item_resource_id != ''
GROUP BY 
    line_item_resource_id, 
    product_servicename, 
    line_item_usage_type,
    line_item_operation,
    product_region
ORDER BY resource_cost DESC
LIMIT 20;

-- Query: EKS cluster costs breakdown
SELECT 
    line_item_resource_id,
    SUM(line_item_unblended_cost) as eks_cost,
    line_item_usage_type,
    COUNT(*) as usage_count
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND product_servicename = 'Amazon Elastic Kubernetes Service'
GROUP BY line_item_resource_id, line_item_usage_type
ORDER BY eks_cost DESC;

-- Query: RDS instance costs breakdown
SELECT 
    line_item_resource_id,
    SUM(line_item_unblended_cost) as rds_cost,
    line_item_usage_type,
    product_instance_type,
    product_database_engine
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND product_servicename = 'Amazon Relational Database Service'
GROUP BY 
    line_item_resource_id, 
    line_item_usage_type,
    product_instance_type,
    product_database_engine
ORDER BY rds_cost DESC;

-- ============================================
-- 4. COST ANOMALY DETECTION
-- ============================================

-- Query: Detect service-level cost anomalies (>20% increase)
WITH this_week AS (
    SELECT 
        product_servicename,
        SUM(line_item_unblended_cost) as cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    GROUP BY product_servicename
),
last_week AS (
    SELECT 
        product_servicename,
        SUM(line_item_unblended_cost) as cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -14, current_date)
        AND line_item_usage_start_date < date_add('day', -7, current_date)
    GROUP BY product_servicename
)
SELECT 
    tw.product_servicename,
    tw.cost as this_week_cost,
    lw.cost as last_week_cost,
    (tw.cost - lw.cost) as cost_increase,
    ((tw.cost - lw.cost) / lw.cost * 100) as percent_change,
    CASE 
        WHEN ((tw.cost - lw.cost) / lw.cost * 100) > 50 THEN 'CRITICAL'
        WHEN ((tw.cost - lw.cost) / lw.cost * 100) > 20 THEN 'WARNING'
        ELSE 'NORMAL'
    END as anomaly_severity
FROM this_week tw
JOIN last_week lw ON tw.product_servicename = lw.product_servicename
WHERE ((tw.cost - lw.cost) / lw.cost * 100) > 20
ORDER BY percent_change DESC;

-- Query: Detect resource-level cost spikes
WITH resource_costs AS (
    SELECT 
        line_item_resource_id,
        product_servicename,
        DATE(line_item_usage_start_date) as usage_date,
        SUM(line_item_unblended_cost) as daily_cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE line_item_usage_start_date >= date_add('day', -14, current_date)
        AND line_item_resource_id IS NOT NULL
    GROUP BY line_item_resource_id, product_servicename, DATE(line_item_usage_start_date)
),
avg_costs AS (
    SELECT 
        line_item_resource_id,
        product_servicename,
        AVG(daily_cost) as avg_daily_cost,
        STDDEV(daily_cost) as stddev_cost
    FROM resource_costs
    GROUP BY line_item_resource_id, product_servicename
)
SELECT 
    rc.line_item_resource_id,
    rc.product_servicename,
    rc.usage_date,
    rc.daily_cost,
    ac.avg_daily_cost,
    (rc.daily_cost - ac.avg_daily_cost) / ac.stddev_cost as z_score
FROM resource_costs rc
JOIN avg_costs ac 
    ON rc.line_item_resource_id = ac.line_item_resource_id
    AND rc.product_servicename = ac.product_servicename
WHERE (rc.daily_cost - ac.avg_daily_cost) / ac.stddev_cost > 2
ORDER BY z_score DESC;

-- ============================================
-- 5. BUDGET FORECAST AND RISK ANALYSIS
-- ============================================

-- Query: Calculate monthly forecast and budget risk
WITH daily_costs AS (
    SELECT 
        DATE(line_item_usage_start_date) as usage_date,
        SUM(line_item_unblended_cost) as daily_cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE MONTH(line_item_usage_start_date) = MONTH(current_date)
        AND YEAR(line_item_usage_start_date) = YEAR(current_date)
    GROUP BY DATE(line_item_usage_start_date)
)
SELECT 
    COUNT(*) as days_elapsed,
    SUM(daily_cost) as month_to_date_cost,
    AVG(daily_cost) as avg_daily_cost,
    AVG(daily_cost) * DAY(LAST_DAY(current_date)) as projected_monthly_cost,
    DAY(LAST_DAY(current_date)) - DAY(current_date) as days_remaining,
    CASE 
        WHEN AVG(daily_cost) * DAY(LAST_DAY(current_date)) > 5000 THEN 'HIGH_RISK'
        WHEN AVG(daily_cost) * DAY(LAST_DAY(current_date)) > 4000 THEN 'MEDIUM_RISK'
        ELSE 'LOW_RISK'
    END as budget_risk_level
FROM daily_costs;

-- Query: Service-level budget allocation analysis
WITH monthly_costs AS (
    SELECT 
        product_servicename,
        SUM(line_item_unblended_cost) as month_to_date_cost,
        COUNT(DISTINCT DATE(line_item_usage_start_date)) as days_with_usage
    FROM cost_usage_reports.cost_usage_reports
    WHERE MONTH(line_item_usage_start_date) = MONTH(current_date)
        AND YEAR(line_item_usage_start_date) = YEAR(current_date)
    GROUP BY product_servicename
)
SELECT 
    product_servicename,
    month_to_date_cost,
    (month_to_date_cost / days_with_usage) * DAY(LAST_DAY(current_date)) as projected_monthly_cost,
    CASE product_servicename
        WHEN 'Amazon Elastic Kubernetes Service' THEN 2000
        WHEN 'Amazon Relational Database Service' THEN 1500
        WHEN 'Amazon ElastiCache' THEN 800
        WHEN 'Amazon Managed Streaming for Apache Kafka' THEN 500
        ELSE 1000
    END as budget_allocation,
    ((month_to_date_cost / days_with_usage) * DAY(LAST_DAY(current_date))) - 
    CASE product_servicename
        WHEN 'Amazon Elastic Kubernetes Service' THEN 2000
        WHEN 'Amazon Relational Database Service' THEN 1500
        WHEN 'Amazon ElastiCache' THEN 800
        WHEN 'Amazon Managed Streaming for Apache Kafka' THEN 500
        ELSE 1000
    END as projected_overspend
FROM monthly_costs
ORDER BY projected_overspend DESC;

-- ============================================
-- 6. REGIONAL COST BREAKDOWN
-- ============================================

-- Query: Cost by region (Taiwan vs Japan)
SELECT 
    product_region,
    SUM(line_item_unblended_cost) as regional_cost,
    COUNT(DISTINCT line_item_resource_id) as resource_count,
    COUNT(DISTINCT product_servicename) as service_count
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND product_region IN ('ap-northeast-1', 'ap-northeast-1')
GROUP BY product_region
ORDER BY regional_cost DESC;

-- Query: Service costs by region
SELECT 
    product_region,
    product_servicename,
    SUM(line_item_unblended_cost) as service_regional_cost
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND product_region IN ('ap-northeast-1', 'ap-northeast-1')
GROUP BY product_region, product_servicename
ORDER BY product_region, service_regional_cost DESC;

-- ============================================
-- 7. USAGE TYPE ANALYSIS
-- ============================================

-- Query: Top usage types by cost
SELECT 
    line_item_usage_type,
    product_servicename,
    SUM(line_item_unblended_cost) as usage_type_cost,
    SUM(line_item_usage_amount) as usage_amount,
    line_item_usage_unit
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
GROUP BY 
    line_item_usage_type, 
    product_servicename,
    line_item_usage_unit
ORDER BY usage_type_cost DESC
LIMIT 20;

-- ============================================
-- 8. COST OPTIMIZATION OPPORTUNITIES
-- ============================================

-- Query: Identify idle resources (low usage, high cost)
SELECT 
    line_item_resource_id,
    product_servicename,
    SUM(line_item_unblended_cost) as total_cost,
    SUM(line_item_usage_amount) as total_usage,
    line_item_usage_unit,
    CASE 
        WHEN SUM(line_item_usage_amount) = 0 THEN 'IDLE'
        WHEN SUM(line_item_usage_amount) < 10 THEN 'LOW_USAGE'
        ELSE 'ACTIVE'
    END as usage_status
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND line_item_resource_id IS NOT NULL
GROUP BY 
    line_item_resource_id, 
    product_servicename,
    line_item_usage_unit
HAVING SUM(line_item_unblended_cost) > 10
    AND SUM(line_item_usage_amount) < 10
ORDER BY total_cost DESC;

-- Query: Reserved Instance vs On-Demand cost comparison
SELECT 
    line_item_line_item_type,
    product_servicename,
    SUM(line_item_unblended_cost) as cost,
    COUNT(*) as usage_count
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -30, current_date)
    AND line_item_line_item_type IN ('Usage', 'DiscountedUsage', 'RIFee')
GROUP BY line_item_line_item_type, product_servicename
ORDER BY product_servicename, cost DESC;

-- ============================================
-- 9. COST ALLOCATION BY TAGS
-- ============================================

-- Query: Cost by environment tag (if tags are configured)
SELECT 
    resource_tags_user_environment as environment,
    SUM(line_item_unblended_cost) as environment_cost,
    COUNT(DISTINCT line_item_resource_id) as resource_count
FROM cost_usage_reports.cost_usage_reports
WHERE line_item_usage_start_date >= date_add('day', -7, current_date)
    AND resource_tags_user_environment IS NOT NULL
GROUP BY resource_tags_user_environment
ORDER BY environment_cost DESC;

-- ============================================
-- 10. EXECUTIVE SUMMARY REPORT
-- ============================================

-- Query: Comprehensive cost summary for executive reporting
WITH current_month AS (
    SELECT 
        SUM(line_item_unblended_cost) as mtd_cost,
        COUNT(DISTINCT line_item_resource_id) as active_resources,
        COUNT(DISTINCT product_servicename) as services_used
    FROM cost_usage_reports.cost_usage_reports
    WHERE MONTH(line_item_usage_start_date) = MONTH(current_date)
        AND YEAR(line_item_usage_start_date) = YEAR(current_date)
),
last_month AS (
    SELECT 
        SUM(line_item_unblended_cost) as total_cost
    FROM cost_usage_reports.cost_usage_reports
    WHERE MONTH(line_item_usage_start_date) = MONTH(date_add('month', -1, current_date))
        AND YEAR(line_item_usage_start_date) = YEAR(date_add('month', -1, current_date))
),
daily_avg AS (
    SELECT 
        AVG(daily_cost) as avg_daily_cost
    FROM (
        SELECT 
            DATE(line_item_usage_start_date) as usage_date,
            SUM(line_item_unblended_cost) as daily_cost
        FROM cost_usage_reports.cost_usage_reports
        WHERE MONTH(line_item_usage_start_date) = MONTH(current_date)
            AND YEAR(line_item_usage_start_date) = YEAR(current_date)
        GROUP BY DATE(line_item_usage_start_date)
    )
)
SELECT 
    cm.mtd_cost as month_to_date_cost,
    lm.total_cost as last_month_cost,
    (cm.mtd_cost - lm.total_cost) / lm.total_cost * 100 as month_over_month_change,
    da.avg_daily_cost * DAY(LAST_DAY(current_date)) as projected_monthly_cost,
    cm.active_resources,
    cm.services_used,
    DAY(current_date) as days_elapsed,
    DAY(LAST_DAY(current_date)) - DAY(current_date) as days_remaining
FROM current_month cm, last_month lm, daily_avg da;
