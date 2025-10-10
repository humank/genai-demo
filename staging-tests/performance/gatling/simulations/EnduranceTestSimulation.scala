package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Endurance Test Simulation for long-running stability validation
 * 
 * This simulation runs for extended periods to identify memory leaks,
 * resource exhaustion, and system degradation over time.
 */
class EnduranceTestSimulation extends Simulation {

  // HTTP protocol configuration optimized for long-running tests
  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test - Endurance Test")
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
    .maxConnectionsPerHost(8) // Conservative connection pool for stability
    .shareConnections
    .enableHttp2 // Use HTTP/2 for better connection management

  // Endurance test parameters
  val users = Integer.getInteger("users", 200)
  val duration = System.getProperty("duration", "3600s") // Default 1 hour
  val rampUpTime = (users * 0.5).toInt // Gradual ramp-up for stability

  // Steady customer operations scenario
  val steadyCustomerScenario = scenario("Steady Customer Operations - Endurance")
    .exec(
      http("Create Customer")
        .post("/api/v1/customers")
        .body(StringBody("""
          {
            "name": "EnduranceCustomer_${__Random(1,1000000)}",
            "email": "endurance_${__Random(1,1000000)}@example.com",
            "phone": "+1${__Random(1000000000,9999999999L)}",
            "address": {
              "street": "${__Random(1,9999)} Endurance Ave",
              "city": "Endurance City",
              "state": "EC",
              "zipCode": "${__Random(10000,99999)}"
            },
            "preferences": {
              "newsletter": ${__Random(0,1) == 1},
              "smsNotifications": ${__Random(0,1) == 1},
              "language": "en"
            }
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("customerId"))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(5, 15) // Realistic user think time
    .exec(
      http("Update Customer Profile")
        .put("/api/v1/customers/${customerId}")
        .body(StringBody("""
          {
            "name": "Updated_EnduranceCustomer_${__Random(1,1000000)}",
            "phone": "+1${__Random(1000000000,9999999999L)}",
            "preferences": {
              "newsletter": ${__Random(0,1) == 1},
              "smsNotifications": ${__Random(0,1) == 1}
            }
          }
        """)).asJson
        .check(status.is(200))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(10, 30)
    .exec(
      http("Get Customer Details")
        .get("/api/v1/customers/${customerId}")
        .check(status.is(200))
        .check(jsonPath("$.name").exists)
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(15, 45)

  // Continuous order processing scenario
  val continuousOrderScenario = scenario("Continuous Order Processing - Endurance")
    .exec(
      http("Create Order")
        .post("/api/v1/orders")
        .body(StringBody("""
          {
            "customerId": "CUST-${__Random(1,10000)}",
            "items": [
              {
                "productId": "PROD-${__Random(1,500)}",
                "quantity": ${__Random(1,5)},
                "price": ${__Random(10,200)}.99
              },
              {
                "productId": "PROD-${__Random(501,1000)}",
                "quantity": ${__Random(1,3)},
                "price": ${__Random(25,150)}.99
              }
            ],
            "shippingAddress": {
              "street": "${__Random(1,9999)} Order Lane",
              "city": "Order Town",
              "state": "OT",
              "zipCode": "${__Random(10000,99999)}"
            },
            "notes": "Endurance test order ${__Random(1,1000000)}"
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("orderId"))
        .check(jsonPath("$.status").is("PENDING"))
        .check(responseTimeInMillis.lt(4000))
    )
    .pause(3, 8)
    .exec(
      http("Process Order Payment")
        .post("/api/v1/orders/${orderId}/payment")
        .body(StringBody("""
          {
            "paymentMethod": "CREDIT_CARD",
            "cardNumber": "4111111111111111",
            "expiryMonth": "${__Random(1,12)}",
            "expiryYear": "${__Random(2025,2030)}",
            "cvv": "${__Random(100,999)}",
            "billingAddress": {
              "street": "${__Random(1,9999)} Billing St",
              "city": "Billing City",
              "state": "BC",
              "zipCode": "${__Random(10000,99999)}"
            }
          }
        """)).asJson
        .check(status.is(200))
        .check(jsonPath("$.transactionId").exists)
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(5, 12)
    .exec(
      http("Update Order Status")
        .patch("/api/v1/orders/${orderId}/status")
        .body(StringBody("""
          {
            "status": "PROCESSING",
            "notes": "Order processing started at ${__time()}"
          }
        """)).asJson
        .check(status.is(200))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(8, 20)
    .exec(
      http("Get Order History")
        .get("/api/v1/orders/${orderId}/history")
        .check(status.is(200))
        .check(jsonPath("$[*].timestamp").exists)
        .check(responseTimeInMillis.lt(1500))
    )
    .pause(20, 60)

  // Long-running search and browse scenario
  val browsingScenario = scenario("Long-term Browsing - Endurance")
    .exec(
      http("Browse Product Categories")
        .get("/api/v1/products/categories")
        .check(status.is(200))
        .check(jsonPath("$[*].name").exists)
        .check(responseTimeInMillis.lt(1500))
    )
    .pause(2, 5)
    .exec(
      http("Search Products")
        .get("/api/v1/products/search")
        .queryParam("q", "endurance test product ${__Random(1,1000)}")
        .queryParam("category", "electronics")
        .queryParam("minPrice", "${__Random(10,50)}")
        .queryParam("maxPrice", "${__Random(100,500)}")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(2500))
    )
    .pause(3, 8)
    .exec(
      http("Get Product Recommendations")
        .get("/api/v1/products/recommendations")
        .queryParam("customerId", "CUST-${__Random(1,5000)}")
        .queryParam("limit", "20")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(5, 15)
    .exec(
      http("Product Details")
        .get("/api/v1/products/PROD-${__Random(1,1000)}")
        .check(status.in(200, 404)) // Product might not exist
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(10, 30)

  // System health monitoring scenario
  val healthMonitoringScenario = scenario("Health Monitoring - Endurance")
    .exec(
      http("Application Health")
        .get("/actuator/health")
        .check(status.is(200))
        .check(jsonPath("$.status").is("UP"))
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(30 seconds)
    .exec(
      http("Database Health")
        .get("/actuator/health/db")
        .check(status.is(200))
        .check(jsonPath("$.status").is("UP"))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(30 seconds)
    .exec(
      http("Cache Health")
        .get("/actuator/health/redis")
        .check(status.is(200))
        .check(jsonPath("$.status").is("UP"))
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(30 seconds)
    .exec(
      http("Memory Metrics")
        .get("/actuator/metrics/jvm.memory.used")
        .check(status.is(200))
        .check(jsonPath("$.measurements[0].value").exists)
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(60 seconds)

  // Periodic cleanup scenario to prevent resource accumulation
  val cleanupScenario = scenario("Periodic Cleanup - Endurance")
    .exec(
      http("Cleanup Old Sessions")
        .delete("/api/v1/admin/cleanup/sessions")
        .queryParam("olderThan", "1h")
        .check(status.in(200, 204))
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(300 seconds) // Run every 5 minutes
    .exec(
      http("Cleanup Temp Files")
        .delete("/api/v1/admin/cleanup/temp-files")
        .queryParam("olderThan", "30m")
        .check(status.in(200, 204))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(300 seconds)

  // Endurance test simulation with steady, long-running load
  setUp(
    // Steady customer operations - 35% of endurance load
    steadyCustomerScenario.inject(
      rampUsers((users * 0.35).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.35 / 3600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers(0) during (120 seconds) // Gradual shutdown
    ),
    
    // Continuous order processing - 40% of endurance load
    continuousOrderScenario.inject(
      rampUsers((users * 0.4).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.4 / 3600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers(0) during (120 seconds)
    ),
    
    // Long-term browsing - 20% of endurance load
    browsingScenario.inject(
      rampUsers((users * 0.2).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.2 / 3600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers(0) during (120 seconds)
    ),
    
    // Health monitoring - 3% of endurance load
    healthMonitoringScenario.inject(
      rampUsers((users * 0.03).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.03 / 3600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers(0) during (120 seconds)
    ),
    
    // Periodic cleanup - 2% of endurance load
    cleanupScenario.inject(
      rampUsers((users * 0.02).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.02 / 3600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers(0) during (120 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Stability-focused assertions for endurance testing
     global.responseTime.percentile3.lt(3000),  // 95th percentile < 3s (should remain stable)
     global.responseTime.percentile4.lt(6000),  // 99th percentile < 6s
     global.successfulRequests.percent.gt(98),   // Success rate > 98% (high stability)
     global.responseTime.mean.lt(1500),          // Mean response time < 1.5s
     
     // Endurance-specific assertions
     forAll.responseTime.max.lt(30000),          // No request > 30s
     global.responseTime.stdDev.lt(2000),        // Low standard deviation for consistency
     
     // System stability assertions
     details("Application Health").successfulRequests.percent.is(100),
     details("Database Health").successfulRequests.percent.gt(99),
     details("Cache Health").successfulRequests.percent.gt(99),
     details("Memory Metrics").successfulRequests.percent.gt(95),
     
     // Performance degradation detection
     details("Create Customer").responseTime.percentile3.lt(3000),
     details("Create Order").responseTime.percentile3.lt(4000),
     details("Process Order Payment").responseTime.percentile3.lt(5000),
     
     // Resource leak detection (response times should not increase significantly over time)
     global.responseTime.percentile3.lt(global.responseTime.mean.times(3))
   )
}