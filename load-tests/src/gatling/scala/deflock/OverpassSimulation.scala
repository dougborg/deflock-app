package deflock

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Gatling simulation for load-testing the Deflock Overpass API endpoint.
 *
 * This simulation validates the performance of overpass.deflock.org before
 * it becomes the primary endpoint for all Deflock app users. It replays
 * realistic queries matching the app's actual request format.
 *
 * == How it works ==
 *
 * A single virtual user walks through zoom levels from tightest (z15, a few
 * city blocks) to widest (z10, a metro region). At each zoom level, it picks
 * a random US city, builds a bounding box around that city's downtown, and
 * sends the same Overpass query the app would send.
 *
 * This progression reveals how response time scales with viewport size —
 * larger viewports return more surveillance nodes, producing bigger responses.
 *
 * == Running ==
 *
 * {{{
 * cd load-tests
 * ./gradlew gatlingRun
 * }}}
 *
 * The HTML report will be in build/reports/gatling/ — open index.html.
 *
 * == Future scenarios (planned) ==
 *
 * - Concurrent users: ramp up multiple virtual users to find capacity limits
 * - Stress test: push beyond expected capacity to find breaking points
 */
class OverpassSimulation extends Simulation {

  // Target our self-hosted Overpass instance (not the public OSMF one).
  // The User-Agent identifies load test traffic in server logs.
  val httpProtocol = http
    .baseUrl("https://overpass.deflock.org")
    .userAgentHeader("DeFlock/LoadTest (+https://deflock.org)")
    .acceptHeader("application/json")

  // Walk through zoom levels from tightest (z15) to widest (z10).
  // At each level, a random city is selected and queried, with a 500ms
  // pause between requests (matching the app's debounce interval).
  //
  // The .reduce(_.exec(_)) chains the zoom-level steps together into a
  // single sequential scenario — Gatling's DSL builds an immutable chain
  // of actions, and reduce folds them left-to-right into one chain.
  val baselineScenario = scenario("Single-user zoom progression")
    .exec(
      TestData.zoomViewports.map { viewport =>
        feed(TestData.feederForZoom(viewport))
          .exec(OverpassRequests.overpassRequest)
          .pause(500.milliseconds)
      }.reduce(_.exec(_))
    )

  // --- Test setup ---
  // atOnceUsers(1): inject exactly 1 virtual user immediately (no ramp-up).
  // This is a baseline test — we want clean, isolated measurements before
  // adding concurrency in future scenarios.
  setUp(
    baselineScenario.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
    .assertions(
      // p99 response time under 30 seconds (generous for Overpass)
      global.responseTime.percentile(99).lt(30000),
      // Less than 5% of requests should fail
      global.failedRequests.percent.lt(5.0)
    )
}
