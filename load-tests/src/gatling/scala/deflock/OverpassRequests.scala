package deflock

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

/**
 * Reusable Overpass API request definitions for Gatling simulations.
 *
 * The request format here must match what the Deflock app actually sends.
 * See lib/services/overpass_service.dart for the app's implementation.
 *
 * Key design decisions:
 * - POST to /api/interpreter with form-encoded body (not GET with query params)
 * - Query includes both surveillance nodes and their parent ways/relations
 * - Timeout matches the app's ResiliencePolicy.httpTimeout (45s server + 5s client margin)
 */
object OverpassRequests {

  // --- Timeouts ---
  // The Overpass QL query tells the server to abort after this many seconds.
  // This matches kOverpassQueryTimeout in the app (lib/dev_config.dart).
  val serverTimeoutSeconds = 45

  // The HTTP client timeout is slightly longer than the server timeout so that
  // we always receive the server's own timeout error response (a 200 with a
  // "remark" field) rather than the client aborting the connection first.
  val clientTimeout = (serverTimeoutSeconds + 5).seconds

  // --- Overpass tag filters ---
  // These match the app's default enabled NodeProfiles. Each filter becomes
  // a separate `node[...]` clause in the Overpass QL query, and the results
  // are unioned together. To test different profiles, add/remove filters here.
  //
  // See: lib/models/node_profile.dart for the full list of app profiles.
  val tagFilters: Seq[String] = Seq(
    """["man_made"="surveillance"]""",
    """["camera:type"="fixed"]"""
  )

  // --- Feeder session keys ---
  // These constants are the variable names injected into each virtual user's
  // session by the feeders in TestData. Using constants here (instead of raw
  // strings) prevents typos that would silently break at runtime.
  val CityName  = "cityName"
  val ZoomLevel = "zoomLevel"
  val QueryBody = "queryBody"

  /**
   * Build an Overpass QL query string for the given bounding box.
   *
   * The query structure matches OverpassService._buildQuery() in the app:
   * 1. Fetch nodes matching any of the tag filters within the bbox
   * 2. Fetch parent ways and relations for those nodes (out skel)
   *
   * Overpass bbox format is (south, west, north, east) — note this is
   * different from many mapping libraries that use (west, south, east, north).
   *
   * @return A complete Overpass QL query string ready to POST
   */
  def buildQuery(south: Double, west: Double, north: Double, east: Double): String = {
    val nodeClauses = tagFilters.map { tags =>
      s"  node$tags($south,$west,$north,$east);"
    }.mkString("\n")

    s"""[out:json][timeout:$serverTimeoutSeconds];
       |(
       |$nodeClauses
       |);
       |out body;
       |(
       |  way(bn);
       |  rel(bn);
       |);
       |out skel;""".stripMargin
  }

  /**
   * The HTTP request definition that Gatling will execute.
   *
   * Uses Gatling's #{...} Expression Language syntax to inject session
   * variables at request time. These variables are populated by the feeders
   * in TestData — see feederForZoom().
   *
   * The request name (e.g., "Overpass z15 - Denver") appears in the Gatling
   * HTML report, making it easy to compare performance across zoom levels
   * and cities.
   *
   * Checks:
   * - HTTP 200 status (Overpass returns 200 even for empty results)
   * - Response body contains an "elements" array (valid Overpass JSON)
   */
  val overpassRequest = http("Overpass z#{zoomLevel} - #{cityName}")
    .post("/api/interpreter")
    .formParam("data", "#{queryBody}")
    .requestTimeout(clientTimeout)
    .check(status.is(200))
    .check(jsonPath("$.elements").exists)
}
