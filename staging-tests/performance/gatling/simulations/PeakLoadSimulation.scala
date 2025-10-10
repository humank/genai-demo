package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Peak Load Simulation for capacity planning
 * 
 * This simulation tests the system under peak load conditions to validate
 * capacity planning and identify performance bottlenecks under high concurrency.
 */
class PeakLoadSimulation extends Simulation {

  // HTTP protocol configuration with connection pooling optimization
  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test - Peak Load")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .connectionHeader("keep-alive")
    .maxConnectionsPerHost(10)
    .shareConnections

  // Test parameters for peak load
  val users = Integer.getInteger("users", 500)
  val duration = System.getProperty("duration", "600s")
  val rampUpTime = (users * 0.2).toInt // Faster ramp-up for peak load

  // High-intensity customer operations
  val intensiveCustomerScenario = scenario("Intensive Customer Operations - Peak Load")
    .exec(
      http("Bulk Customer Creation")
        .post("/api/v1/customers/batch")
        .body(StringBody("""
          {
            "customers": [
              {
                "name": "PeakCustomer1_${__Random(1,10000)}",
                "email": "peak1_${__Random(1,10000)}@example.com",
                "phone": "+1${__Random(1000000000,9999999999L)}"
              },
              {
                "name": "PeakCustomer2_${__Random(1,10000)}",
                "email": "peak2_${__Random(1,10000)}@example.com",
                "phone": "+1${__Random(1000000000,9999999999L)}"
              },
              {
                "name": "PeakCustomer3_${__Random(1,10000)}",
                "email": "peak3_${__Random(1,10000)}@example.com",
                "phone": "+1${__Random(1000000000,9999999999L)}"
              }
            ]
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$[*].id").findAll.saveAs("customerIds"))
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(500 milliseconds, 1500 milliseconds) // Shorter think time for peak load
    .exec(
      http("Customer Search")
        .get("/api/v1/customers/search")
        .queryParam("q", "PeakCustomer")
        .queryParam("limit", "50")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(3000))
    )

  // High-volume order processing
  val highVolumeOrderScenario = scenario("High Volume Orders - Peak Load")
    .exec(
      http("Express Order Creation")
        .post("/api/v1/orders/express")
        .body(StringBody("""
          {
            "customerId": "CUST-${__Random(1,2000)}",
            "priority": "HIGH",
            "items": [
              {
                "productId": "PROD-${__Random(1,200)}",
                "quantity": ${__Random(1,10)},
                "price": ${__Random(20,200)}.99
              },
              {
                "productId": "PROD-${__Random(1,200)}",
                "quantity": ${__Random(1,8)},
                "price": ${__Random(30,300)}.99
              },
              {
                "productId": "PROD-${__Random(1,200)}",
                "quantity": ${__Random(1,5)},
                "price": ${__Random(50,500)}.99
              }
            ],
            "expeditedShipping": true
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("orderId"))
        .check(jsonPath("$.estimatedDelivery").exists)
        .check(responseTimeInMillis.lt(4000))
    )
    .pause(200 milliseconds, 800 milliseconds)
    .exec(
      http("Order Status Check")
        .get("/api/v1/orders/${orderId}/status")
        .check(status.is(200))
        .check(jsonPath("$.status").exists)
        .check(responseTimeInMillis.lt(1500))
    )
    .pause(300 milliseconds, 1000 milliseconds)
    .exec(
      http("Order Payment Processing")
        .post("/api/v1/orders/${orderId}/payment")
        .body(StringBody("""
          {
            "paymentMethod": "CREDIT_CARD",
            "cardNumber": "4111111111111111",
            "expiryMonth": "12",
            "expiryYear": "2025",
            "cvv": "123",
            "amount": ${__Random(100,1000)}.99
          }
        """)).asJson
        .check(status.is(200))
        .check(jsonPath("$.transactionId").exists)
        .check(responseTimeInMillis.lt(6000))
    )

  // Concurrent product catalog access
  val catalogConcurrencyScenario = scenario("Catalog Concurrency - Peak Load")
    .exec(
      http("Popular Products")
        .get("/api/v1/products/popular")
        .queryParam("limit", "100")
        .check(status.is(200))
        .check(jsonPath("$[*].id").findAll.saveAs("popularProductIds"))
        .check(responseTimeInMillis.lt(2500))
    )
    .pause(100 milliseconds, 500 milliseconds)
    .exec(
      http("Product Details Batch")
        .get("/api/v1/products/batch")
        .queryParam("ids", "PROD-${__Random(1,50)},PROD-${__Random(51,100)},PROD-${__Random(101,150)}")
        .check(status.is(200))
        .check(jsonPath("$[*].name").findAll.exists)
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(200 milliseconds, 600 milliseconds)
    .exec(
      http("Category Products")
        .get("/api/v1/products/category/electronics")
        .queryParam("page", "${__Random(0,10)}")
        .queryParam("size", "50")
        .queryParam("sort", "popularity,desc")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(3000))
    )

  // System stress monitoring scenario
  val systemMonitoringScenario = scenario("System Monitoring - Peak Load")
    .exec(
      http("System Metrics")
        .get("/actuator/metrics")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(5 seconds)
    .exec(
      http("Database Health")
        .get("/actuator/health/db")
        .check(status.is(200))
        .check(jsonPath("$.status").is("UP"))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(5 seconds)
    .exec(
      http("Cache Health")
        .get("/actuator/health/redis")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(1000))
    )

  // Peak load simulation with aggressive ramp-up
  setUp(
    // Intensive customer operations - 30% of peak load
    intensiveCustomerScenario.inject(
      rampUsers((users * 0.3).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.3 / 600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers((users * 0.15).toInt) during (60 seconds), // Spike in the middle
      constantUsersPerSec((users * 0.3 / 600).toDouble) during (60 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // High-volume orders - 40% of peak load
    highVolumeOrderScenario.inject(
      rampUsers((users * 0.4).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.4 / 600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers((users * 0.2).toInt) during (60 seconds), // Order spike
      constantUsersPerSec((users * 0.4 / 600).toDouble) during (60 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // Catalog concurrency - 25% of peak load
    catalogConcurrencyScenario.inject(
      rampUsers((users * 0.25).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.25 / 600).toDouble) during (duration.toInt - 120 seconds),
      rampUsers((users * 0.125).toInt) during (60 seconds),
      constantUsersPerSec((users * 0.25 / 600).toDouble) during (60 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // System monitoring - 5% of peak load
    systemMonitoringScenario.inject(
      rampUsers((users * 0.05).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.05 / 600).toDouble) during (duration.toInt - 60 seconds),
      rampUsers(0) during (60 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     // More relaxed assertions for peak load
     global.responseTime.percentile3.lt(4000),  // 95th percentile < 4s
     global.responseTime.percentile4.lt(8000),  // 99th percentile < 8s
     global.successfulRequests.percent.gt(95),   // Success rate > 95%
     global.responseTime.mean.lt(2500),          // Mean response time < 2.5s
     
     // Peak load specific assertions
     forAll.responseTime.max.lt(15000),          // No request > 15s
     details("Express Order Creation").responseTime.percentile3.lt(4000),
     details("Order Payment Processing").responseTime.percentile3.lt(6000),
     details("Popular Products").responseTime.percentile3.lt(2500),
     
     // Throughput assertions
     global.requestsPerSec.gte(users * 0.8), // Minimum throughput
     details("Bulk Customer Creation").requestsPerSec.gte(users * 0.1)
   )
}