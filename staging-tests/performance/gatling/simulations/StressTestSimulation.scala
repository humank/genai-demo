package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Stress Test Simulation for breaking point identification
 * 
 * This simulation pushes the system beyond normal capacity to identify
 * breaking points, failure modes, and system behavior under extreme load.
 */
class StressTestSimulation extends Simulation {

  // HTTP protocol configuration optimized for stress testing
  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test - Stress Test")
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
    .maxConnectionsPerHost(20) // Higher connection pool for stress
    .shareConnections
    .disableWarmUp // Skip warmup for immediate stress

  // Stress test parameters
  val users = Integer.getInteger("users", 1000)
  val duration = System.getProperty("duration", "900s")
  val rampUpTime = (users * 0.1).toInt // Very aggressive ramp-up

  // Extreme customer load scenario
  val extremeCustomerLoadScenario = scenario("Extreme Customer Load - Stress Test")
    .exec(
      http("Massive Customer Creation")
        .post("/api/v1/customers/batch")
        .body(StringBody("""
          {
            "customers": [
              ${__range(1, 10, i =>
                s"""
                {
                  "name": "StressCustomer${i}_$${__Random(1,100000)}",
                  "email": "stress${i}_$${__Random(1,100000)}@example.com",
                  "phone": "+1$${__Random(1000000000,9999999999L)}",
                  "address": {
                    "street": "$${__Random(1,9999)} Stress St",
                    "city": "Stress City",
                    "state": "ST",
                    "zipCode": "$${__Random(10000,99999)}"
                  }
                }
                """
              ).mkString(",")}
            ]
          }
        """)).asJson
        .check(status.in(200, 201, 429, 503)) // Accept rate limiting and service unavailable
        .check(responseTimeInMillis.lt(30000)) // Very generous timeout for stress
    )
    .pause(50 milliseconds, 200 milliseconds) // Minimal think time
    .exec(
      http("Customer Bulk Update")
        .put("/api/v1/customers/bulk-update")
        .body(StringBody("""
          {
            "updates": [
              ${__range(1, 20, i =>
                s"""
                {
                  "customerId": "CUST-$${__Random(1,5000)}",
                  "name": "UpdatedStressCustomer${i}",
                  "email": "updated_stress${i}_$${__Random(1,100000)}@example.com"
                }
                """
              ).mkString(",")}
            ]
          }
        """)).asJson
        .check(status.in(200, 207, 429, 503)) // Multi-status, rate limiting acceptable
        .check(responseTimeInMillis.lt(45000))
    )

  // Database stress scenario
  val databaseStressScenario = scenario("Database Stress - Stress Test")
    .exec(
      http("Complex Query Stress")
        .get("/api/v1/analytics/complex-report")
        .queryParam("startDate", "2024-01-01")
        .queryParam("endDate", "2024-12-31")
        .queryParam("includeDetails", "true")
        .queryParam("groupBy", "month,category,region")
        .check(status.in(200, 408, 429, 503)) // Timeout and rate limiting acceptable
        .check(responseTimeInMillis.lt(60000)) // Very long timeout for complex queries
    )
    .pause(100 milliseconds, 300 milliseconds)
    .exec(
      http("Concurrent Data Export")
        .post("/api/v1/data/export")
        .body(StringBody("""
          {
            "format": "CSV",
            "tables": ["customers", "orders", "products", "transactions"],
            "dateRange": {
              "start": "2024-01-01",
              "end": "2024-12-31"
            },
            "includeArchived": true
          }
        """)).asJson
        .check(status.in(200, 202, 429, 503)) // Async processing acceptable
        .check(responseTimeInMillis.lt(30000))
    )

  // Memory stress scenario
  val memoryStressScenario = scenario("Memory Stress - Stress Test")
    .exec(
      http("Large Payload Processing")
        .post("/api/v1/data/process-large")
        .body(StringBody("""
          {
            "data": "${__Random(1,1000000).toString * 1000}", 
            "operations": [
              "VALIDATE",
              "TRANSFORM",
              "AGGREGATE",
              "SORT",
              "DEDUPLICATE"
            ],
            "outputFormat": "JSON",
            "compressionLevel": 9
          }
        """)).asJson
        .check(status.in(200, 413, 429, 503)) // Payload too large acceptable
        .check(responseTimeInMillis.lt(120000)) // Very long timeout for large processing
    )
    .pause(200 milliseconds, 500 milliseconds)
    .exec(
      http("Memory Intensive Search")
        .get("/api/v1/search/advanced")
        .queryParam("query", "stress test data ${__Random(1,10000)}")
        .queryParam("fuzzy", "true")
        .queryParam("includeContent", "true")
        .queryParam("maxResults", "10000")
        .queryParam("sortBy", "relevance")
        .check(status.in(200, 413, 429, 503))
        .check(responseTimeInMillis.lt(90000))
    )

  // Connection pool stress scenario
  val connectionStressScenario = scenario("Connection Pool Stress - Stress Test")
    .exec(
      http("Concurrent Database Operations")
        .post("/api/v1/database/concurrent-ops")
        .body(StringBody("""
          {
            "operations": [
              ${__range(1, 50, i =>
                s"""
                {
                  "type": "SELECT",
                  "table": "stress_test_table_${i % 10}",
                  "conditions": {
                    "id": $${__Random(1,100000)},
                    "status": "ACTIVE"
                  }
                }
                """
              ).mkString(",")}
            ],
            "transactional": true,
            "timeout": 30000
          }
        """)).asJson
        .check(status.in(200, 408, 429, 503, 507)) // Various failure modes acceptable
        .check(responseTimeInMillis.lt(35000))
    )
    .pause(50 milliseconds, 150 milliseconds)

  // System resource monitoring under stress
  val stressMonitoringScenario = scenario("Stress Monitoring - Stress Test")
    .exec(
      http("System Resource Check")
        .get("/actuator/metrics/system.cpu.usage")
        .check(status.in(200, 503))
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(2 seconds)
    .exec(
      http("Memory Usage Check")
        .get("/actuator/metrics/jvm.memory.used")
        .check(status.in(200, 503))
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(2 seconds)
    .exec(
      http("Thread Pool Status")
        .get("/actuator/metrics/executor.active")
        .check(status.in(200, 503))
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(3 seconds)

  // Stress test simulation with extreme load patterns
  setUp(
    // Extreme customer load - 25% of stress load
    extremeCustomerLoadScenario.inject(
      rampUsers((users * 0.25).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.25 / 900).toDouble) during (300 seconds),
      rampUsers((users * 0.5).toInt) during (60 seconds), // Massive spike
      constantUsersPerSec((users * 0.75 / 900).toDouble) during (240 seconds), // Sustained high load
      rampUsers((users * 0.25).toInt) during (60 seconds), // Another spike
      constantUsersPerSec((users * 0.25 / 900).toDouble) during (240 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // Database stress - 30% of stress load
    databaseStressScenario.inject(
      rampUsers((users * 0.3).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.3 / 900).toDouble) during (300 seconds),
      rampUsers((users * 0.6).toInt) during (60 seconds), // Database spike
      constantUsersPerSec((users * 0.9 / 900).toDouble) during (240 seconds),
      rampUsers((users * 0.3).toInt) during (60 seconds),
      constantUsersPerSec((users * 0.3 / 900).toDouble) during (240 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // Memory stress - 25% of stress load
    memoryStressScenario.inject(
      rampUsers((users * 0.25).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.25 / 900).toDouble) during (300 seconds),
      rampUsers((users * 0.5).toInt) during (60 seconds), // Memory spike
      constantUsersPerSec((users * 0.75 / 900).toDouble) during (240 seconds),
      rampUsers((users * 0.25).toInt) during (60 seconds),
      constantUsersPerSec((users * 0.25 / 900).toDouble) during (240 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // Connection stress - 15% of stress load
    connectionStressScenario.inject(
      rampUsers((users * 0.15).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.15 / 900).toDouble) during (300 seconds),
      rampUsers((users * 0.3).toInt) during (60 seconds), // Connection spike
      constantUsersPerSec((users * 0.45 / 900).toDouble) during (240 seconds),
      rampUsers((users * 0.15).toInt) during (60 seconds),
      constantUsersPerSec((users * 0.15 / 900).toDouble) during (240 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // Stress monitoring - 5% of stress load
    stressMonitoringScenario.inject(
      rampUsers((users * 0.05).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.05 / 900).toDouble) during (duration.toInt - 60 seconds),
      rampUsers(0) during (60 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Relaxed assertions for stress testing - focus on system survival
     global.responseTime.percentile3.lt(10000), // 95th percentile < 10s
     global.responseTime.percentile4.lt(30000), // 99th percentile < 30s
     global.successfulRequests.percent.gt(70),   // Success rate > 70% (failures expected)
     global.responseTime.mean.lt(5000),          // Mean response time < 5s
     
     // Stress test specific assertions - focus on not crashing
     forAll.responseTime.max.lt(120000),         // No request > 2 minutes
     global.failedRequests.percent.lt(30),       // Failure rate < 30%
     
     // System survival assertions
     details("System Resource Check").successfulRequests.percent.gt(80),
     details("Memory Usage Check").successfulRequests.percent.gt(80),
     details("Thread Pool Status").successfulRequests.percent.gt(80)
   )
}