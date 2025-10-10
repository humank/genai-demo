package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Spike Test Simulation for sudden load surge testing
 * 
 * This simulation tests the system's ability to handle sudden spikes in traffic,
 * simulating scenarios like flash sales, viral content, or marketing campaigns.
 */
class SpikeTestSimulation extends Simulation {

  // HTTP protocol configuration for spike testing
  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test - Spike Test")
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
    .maxConnectionsPerHost(25) // High connection pool for spike
    .shareConnections
    .disableWarmUp

  // Spike test parameters
  val users = Integer.getInteger("users", 2000)
  val duration = System.getProperty("duration", "180s") // Short duration, high intensity
  val spikeUsers = (users * 1.5).toInt // 150% of normal users for spike

  // Flash sale scenario - simulates sudden product demand
  val flashSaleScenario = scenario("Flash Sale Spike - Spike Test")
    .exec(
      http("Check Flash Sale Status")
        .get("/api/v1/sales/flash-sale/status")
        .check(status.is(200))
        .check(jsonPath("$.active").is("true"))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(100 milliseconds, 500 milliseconds)
    .exec(
      http("Get Flash Sale Products")
        .get("/api/v1/sales/flash-sale/products")
        .check(status.is(200))
        .check(jsonPath("$[*].id").findAll.saveAs("saleProductIds"))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(200 milliseconds, 800 milliseconds)
    .exec(
      http("Add to Cart - Flash Sale")
        .post("/api/v1/cart/add")
        .body(StringBody("""
          {
            "customerId": "CUST-${__Random(1,10000)}",
            "productId": "PROD-${__Random(1,50)}", 
            "quantity": ${__Random(1,3)},
            "salePrice": ${__Random(5,50)}.99,
            "flashSale": true
          }
        """)).asJson
        .check(status.in(200, 409, 429)) // Conflict for out of stock, rate limiting
        .check(responseTimeInMillis.lt(5000))
    )
    .pause(300 milliseconds, 1000 milliseconds)
    .exec(
      http("Quick Checkout")
        .post("/api/v1/checkout/express")
        .body(StringBody("""
          {
            "customerId": "CUST-${__Random(1,10000)}",
            "paymentMethod": "SAVED_CARD",
            "shippingMethod": "STANDARD",
            "promoCode": "FLASH50"
          }
        """)).asJson
        .check(status.in(200, 201, 409, 429, 503))
        .check(responseTimeInMillis.lt(8000))
    )

  // Viral content scenario - simulates sudden content popularity
  val viralContentScenario = scenario("Viral Content Spike - Spike Test")
    .exec(
      http("Get Trending Content")
        .get("/api/v1/content/trending")
        .queryParam("limit", "10")
        .check(status.is(200))
        .check(jsonPath("$[*].id").findAll.saveAs("trendingIds"))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(50 milliseconds, 200 milliseconds)
    .exec(
      http("View Viral Content")
        .get("/api/v1/content/CONTENT-${__Random(1,20)}")
        .check(status.in(200, 404))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(100 milliseconds, 400 milliseconds)
    .exec(
      http("Share Content")
        .post("/api/v1/content/share")
        .body(StringBody("""
          {
            "contentId": "CONTENT-${__Random(1,20)}",
            "userId": "USER-${__Random(1,50000)}",
            "platform": "SOCIAL_MEDIA",
            "message": "Check this out! Spike test content ${__Random(1,10000)}"
          }
        """)).asJson
        .check(status.in(200, 201, 429))
        .check(responseTimeInMillis.lt(4000))
    )
    .pause(200 milliseconds, 600 milliseconds)
    .exec(
      http("Like Content")
        .post("/api/v1/content/CONTENT-${__Random(1,20)}/like")
        .body(StringBody("""
          {
            "userId": "USER-${__Random(1,50000)}"
          }
        """)).asJson
        .check(status.in(200, 201, 409, 429))
        .check(responseTimeInMillis.lt(2000))
    )

  // Registration surge scenario - simulates sudden user registrations
  val registrationSurgeScenario = scenario("Registration Surge - Spike Test")
    .exec(
      http("Quick Registration")
        .post("/api/v1/users/quick-register")
        .body(StringBody("""
          {
            "email": "spike_user_${__Random(1,1000000)}@example.com",
            "password": "SpikeTest123!",
            "firstName": "Spike",
            "lastName": "User${__Random(1,100000)}",
            "source": "SPIKE_CAMPAIGN",
            "acceptTerms": true
          }
        """)).asJson
        .check(status.in(201, 409, 429)) // Conflict for duplicate email
        .check(jsonPath("$.userId").optional.saveAs("newUserId"))
        .check(responseTimeInMillis.lt(6000))
    )
    .pause(200 milliseconds, 800 milliseconds)
    .doIf(session => session.contains("newUserId")) {
      exec(
        http("Complete Profile")
          .put("/api/v1/users/${newUserId}/profile")
          .body(StringBody("""
            {
              "phone": "+1${__Random(1000000000,9999999999L)}",
              "dateOfBirth": "199${__Random(0,9)}-${__Random(1,12).toString.padTo(2, '0')}-${__Random(1,28).toString.padTo(2, '0')}",
              "preferences": {
                "newsletter": true,
                "sms": false
              }
            }
          """)).asJson
          .check(status.in(200, 429))
          .check(responseTimeInMillis.lt(4000))
      )
    }

  // API rate limiting test scenario
  val rateLimitingScenario = scenario("Rate Limiting Test - Spike Test")
    .exec(
      http("Rapid API Calls 1")
        .get("/api/v1/products/popular")
        .check(status.in(200, 429))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(10 milliseconds, 50 milliseconds)
    .exec(
      http("Rapid API Calls 2")
        .get("/api/v1/products/categories")
        .check(status.in(200, 429))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(10 milliseconds, 50 milliseconds)
    .exec(
      http("Rapid API Calls 3")
        .get("/api/v1/products/search")
        .queryParam("q", "spike")
        .check(status.in(200, 429))
        .check(responseTimeInMillis.lt(3000))
    )
    .pause(10 milliseconds, 50 milliseconds)
    .exec(
      http("Rapid API Calls 4")
        .get("/api/v1/users/profile")
        .queryParam("userId", "USER-${__Random(1,1000)}")
        .check(status.in(200, 404, 429))
        .check(responseTimeInMillis.lt(3000))
    )

  // System monitoring during spike
  val spikeMonitoringScenario = scenario("Spike Monitoring - Spike Test")
    .exec(
      http("CPU Usage During Spike")
        .get("/actuator/metrics/system.cpu.usage")
        .check(status.in(200, 503))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(5 seconds)
    .exec(
      http("Memory Usage During Spike")
        .get("/actuator/metrics/jvm.memory.used")
        .check(status.in(200, 503))
        .check(responseTimeInMillis.lt(2000))
    )
    .pause(5 seconds)
    .exec(
      http("Active Connections During Spike")
        .get("/actuator/metrics/tomcat.sessions.active.current")
        .check(status.in(200, 503))
        .check(responseTimeInMillis.lt(2000))
    )

  // Spike test simulation with sudden load surges
  setUp(
    // Flash sale spike - 40% of spike load
    flashSaleScenario.inject(
      nothingFor(10 seconds), // Calm before the storm
      rampUsers((spikeUsers * 0.4).toInt) during (20 seconds), // Sudden spike
      constantUsersPerSec((spikeUsers * 0.4 / 60).toDouble) during (60 seconds), // Sustained spike
      rampUsers((spikeUsers * 0.2).toInt) during (10 seconds), // Additional surge
      constantUsersPerSec((spikeUsers * 0.6 / 60).toDouble) during (30 seconds), // Peak load
      rampUsers(0) during (60 seconds) // Gradual decline
    ),
    
    // Viral content spike - 25% of spike load
    viralContentScenario.inject(
      nothingFor(15 seconds),
      rampUsers((spikeUsers * 0.25).toInt) during (15 seconds), // Viral spread
      constantUsersPerSec((spikeUsers * 0.25 / 60).toDouble) during (60 seconds),
      rampUsers((spikeUsers * 0.125).toInt) during (10 seconds), // Secondary viral wave
      constantUsersPerSec((spikeUsers * 0.375 / 60).toDouble) during (30 seconds),
      rampUsers(0) during (60 seconds)
    ),
    
    // Registration surge - 20% of spike load
    registrationSurgeScenario.inject(
      nothingFor(20 seconds),
      rampUsers((spikeUsers * 0.2).toInt) during (30 seconds), // Registration campaign effect
      constantUsersPerSec((spikeUsers * 0.2 / 60).toDouble) during (60 seconds),
      rampUsers(0) during (70 seconds)
    ),
    
    // Rate limiting test - 10% of spike load
    rateLimitingScenario.inject(
      nothingFor(5 seconds),
      rampUsers((spikeUsers * 0.1).toInt) during (10 seconds), // Immediate high frequency
      constantUsersPerSec((spikeUsers * 0.1 / 60).toDouble) during (90 seconds),
      rampUsers(0) during (75 seconds)
    ),
    
    // Spike monitoring - 5% of spike load
    spikeMonitoringScenario.inject(
      rampUsers((spikeUsers * 0.05).toInt) during (10 seconds),
      constantUsersPerSec((spikeUsers * 0.05 / 60).toDouble) during (duration.toInt - 20 seconds),
      rampUsers(0) during (10 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     // Spike-specific assertions - system should handle sudden load
     global.responseTime.percentile3.lt(8000),  // 95th percentile < 8s during spike
     global.responseTime.percentile4.lt(15000), // 99th percentile < 15s during spike
     global.successfulRequests.percent.gt(85),   // Success rate > 85% (some failures expected)
     global.responseTime.mean.lt(4000),          // Mean response time < 4s
     
     // Spike recovery assertions
     forAll.responseTime.max.lt(30000),          // No request > 30s
     global.failedRequests.percent.lt(15),       // Failure rate < 15%
     
     // Rate limiting effectiveness
     details("Rapid API Calls 1").failedRequests.percent.gt(10), // Some rate limiting expected
     details("Rapid API Calls 2").failedRequests.percent.gt(10),
     
     // System survival during spike
     details("CPU Usage During Spike").successfulRequests.percent.gt(70),
     details("Memory Usage During Spike").successfulRequests.percent.gt(70),
     
     // Business critical operations should still work
     details("Quick Registration").successfulRequests.percent.gt(80),
     details("Quick Checkout").successfulRequests.percent.gt(75)
   )
}