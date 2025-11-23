package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Normal Load Simulation for baseline performance testing
 * 
 * This simulation represents typical production load with realistic user patterns.
 * It tests the system under normal operating conditions to establish performance baselines.
 */
class NormalLoadSimulation extends Simulation {

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test - Normal Load")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")

  // Test parameters
  val users = Integer.getInteger("users", 100)
  val duration = System.getProperty("duration", "300s")
  val rampUpTime = (users * 0.3).toInt // 30% of users for ramp-up

  // Customer creation scenario
  val customerCreationScenario = scenario("Customer Creation - Normal Load")
    .exec(
      http("Create Customer")
        .post("/api/v1/customers")
        .body(StringBody("""
          {
            "name": "Customer ${__Random(1,10000)}",
            "email": "customer${__Random(1,10000)}@example.com",
            "phone": "+1${__Random(1000000000,9999999999L)}",
            "address": {
              "street": "${__Random(1,999)} Main St",
              "city": "Test City",
              "state": "TS",
              "zipCode": "${__Random(10000,99999)}"
            }
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("customerId"))
        .check(responseTimeInMillis.lt(2000)) // Response time assertion
    )
    .pause(1, 3) // Think time between requests
    .exec(
      http("Get Customer")
        .get("/api/v1/customers/${customerId}")
        .check(status.is(200))
        .check(jsonPath("$.name").exists)
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(2, 5)

  // Order creation scenario
  val orderCreationScenario = scenario("Order Creation - Normal Load")
    .exec(
      http("Create Order")
        .post("/api/v1/orders")
        .body(StringBody("""
          {
            "customerId": "CUST-${__Random(1,1000)}",
            "items": [
              {
                "productId": "PROD-${__Random(1,100)}",
                "quantity": ${__Random(1,5)},
                "price": ${__Random(10,100)}.99
              },
              {
                "productId": "PROD-${__Random(1,100)}",
                "quantity": ${__Random(1,3)},
                "price": ${__Random(15,150)}.99
              }
            ],
            "shippingAddress": {
              "street": "${__Random(1,999)} Order St",
              "city": "Order City",
              "state": "OS",
              "zipCode": "${__Random(10000,99999)}"
            }
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("orderId"))
        .check(jsonPath("$.status").is("PENDING"))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(1, 2)
    .exec(
      http("Get Order")
        .get("/api/v1/orders/${orderId}")
        .check(status.is(200))
        .check(jsonPath("$.status").exists)
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(2, 4)

  // Product browsing scenario
  val productBrowsingScenario = scenario("Product Browsing - Normal Load")
    .exec(
      http("List Products")
        .get("/api/v1/products")
        .queryParam("page", "0")
        .queryParam("size", "20")
        .check(status.is(200))
        .check(jsonPath("$.content").exists)
        .check(responseTimeInMillis.lt(1500))
    )
    .pause(1, 2)
    .exec(
      http("Get Product Details")
        .get("/api/v1/products/PROD-${__Random(1,100)}")
        .check(status.in(200, 404)) // Product might not exist
        .check(responseTimeInMillis.lt(1000))
    )
    .pause(2, 3)
    .exec(
      http("Search Products")
        .get("/api/v1/products/search")
        .queryParam("q", "test product")
        .queryParam("category", "electronics")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(2000))
    )

  // Health check scenario (lightweight monitoring)
  val healthCheckScenario = scenario("Health Check - Normal Load")
    .exec(
      http("Application Health")
        .get("/actuator/health")
        .check(status.is(200))
        .check(jsonPath("$.status").is("UP"))
        .check(responseTimeInMillis.lt(500))
    )
    .pause(10, 15) // Less frequent health checks

  // Load simulation setup
  setUp(
    // Customer operations - 40% of load
    customerCreationScenario.inject(
      rampUsers((users * 0.4).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.4 / 300).toDouble) during (duration.toInt - 60 seconds),
      rampUsers(0) during (30 seconds)
    ),
    
    // Order operations - 35% of load
    orderCreationScenario.inject(
      rampUsers((users * 0.35).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.35 / 300).toDouble) during (duration.toInt - 60 seconds),
      rampUsers(0) during (30 seconds)
    ),
    
    // Product browsing - 20% of load
    productBrowsingScenario.inject(
      rampUsers((users * 0.2).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.2 / 300).toDouble) during (duration.toInt - 60 seconds),
      rampUsers(0) during (30 seconds)
    ),
    
    // Health checks - 5% of load
    healthCheckScenario.inject(
      rampUsers((users * 0.05).toInt) during (rampUpTime seconds),
      constantUsersPerSec((users * 0.05 / 300).toDouble) during (duration.toInt - 60 seconds),
      rampUsers(0) during (30 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Global performance assertions
     global.responseTime.percentile3.lt(2000),  // 95th percentile < 2s
     global.responseTime.percentile4.lt(5000),  // 99th percentile < 5s
     global.successfulRequests.percent.gt(99),   // Success rate > 99%
     global.responseTime.mean.lt(1000),          // Mean response time < 1s
     
     // Specific scenario assertions
     forAll.responseTime.max.lt(10000),          // No request > 10s
     details("Create Customer").responseTime.percentile3.lt(2000),
     details("Create Order").responseTime.percentile3.lt(3000),
     details("List Products").responseTime.percentile3.lt(1500)
   )
}