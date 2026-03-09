package deflock

import io.gatling.core.Predef._

/**
 * Center coordinates for a city's downtown area.
 *
 * These are the starting points for building map viewport bounding boxes.
 * Each coordinate was verified against map data and chosen for its high
 * density of surveillance infrastructure (cameras, ALPR, etc.), which
 * produces realistic Overpass API response sizes.
 *
 * @param name  Human-readable city name (appears in Gatling report labels)
 * @param lat   Latitude of the downtown center point
 * @param lng   Longitude of the downtown center point
 */
case class CityCenter(name: String, lat: Double, lng: Double)

/**
 * The dimensions of a map viewport at a given zoom level.
 *
 * These represent what a user sees on their phone screen at each zoom level.
 * Larger viewports (lower zoom) fetch more data from the Overpass API, so
 * we use these to measure how response time scales with area.
 *
 * @param zoom     OSM/Slippy map zoom level (10 = metro region, 15 = a few blocks)
 * @param latSpan  Height of the viewport in degrees of latitude
 * @param lngSpan  Width of the viewport in degrees of longitude
 */
case class ZoomViewport(zoom: Int, latSpan: Double, lngSpan: Double)

object TestData {

  /**
   * US cities with verified downtown coordinates targeting high-surveillance areas.
   *
   * Each city was chosen because its downtown has significant camera density,
   * producing realistic query results. The coordinates point to specific
   * well-known locations in each city's central business district.
   */
  val cities: Seq[CityCenter] = Seq(
    CityCenter("Denver",        39.7478, -104.9995), // 16th St Mall / Union Station
    CityCenter("Los Angeles",   34.0483, -118.2530), // Pershing Square, DTLA
    CityCenter("San Francisco", 37.7946, -122.3999), // Financial District / Market & Montgomery
    CityCenter("New York",      40.7549,  -73.9840), // Midtown / 42nd & 6th Ave
    CityCenter("Boston",        42.3567,  -71.0588), // Downtown Crossing
    CityCenter("Chicago",       41.8783,  -87.6258)  // State & Madison, The Loop
  )

  /**
   * Map viewport sizes for zoom levels 10 through 15.
   *
   * Calculated for a ~400x800px mobile screen (portrait orientation) at ~40 deg N
   * latitude using standard OSM/Slippy map tile math (Mercator projection,
   * 256px tiles). Each zoom level doubles the tile count, halving the viewport span.
   *
   * | Zoom | Approx area covered   | Example                      |
   * |------|-----------------------|------------------------------|
   * |  15  | ~1.5 x 3 km           | A few city blocks            |
   * |  14  | ~3 x 6 km             | A neighborhood               |
   * |  13  | ~6 x 12 km            | A district                   |
   * |  12  | ~12 x 23 km           | A mid-size city              |
   * |  11  | ~23 x 47 km           | A large city extent          |
   * |  10  | ~47 x 93 km           | A metro region               |
   *
   * Ordered from tightest to widest so the simulation can walk through them
   * and show the performance impact of increasing viewport size.
   */
  val zoomViewports: Seq[ZoomViewport] = Seq(
    ZoomViewport(15, 0.026, 0.017),
    ZoomViewport(14, 0.053, 0.034),
    ZoomViewport(13, 0.105, 0.069),
    ZoomViewport(12, 0.210, 0.140),
    ZoomViewport(11, 0.420, 0.270),
    ZoomViewport(10, 0.840, 0.550)
  )

  /**
   * Create a Gatling feeder that picks a random city for a given zoom level.
   *
   * A "feeder" in Gatling is a data source that injects variables into the
   * virtual user's session before each request. This one pre-computes the
   * Overpass query body so it's built once at startup, not on every request.
   *
   * The bounding box is computed by centering the viewport on the city's
   * downtown coordinates: south/north = lat +/- half the latSpan, etc.
   *
   * @param viewport The zoom level and its corresponding viewport dimensions
   * @return A Gatling feeder that randomly selects a city and provides session
   *         variables: cityName, zoomLevel, and the pre-built queryBody
   */
  def feederForZoom(viewport: ZoomViewport) = cities.map { city =>
    val south = city.lat - viewport.latSpan / 2
    val north = city.lat + viewport.latSpan / 2
    val west  = city.lng - viewport.lngSpan / 2
    val east  = city.lng + viewport.lngSpan / 2

    Map(
      OverpassRequests.CityName  -> city.name,
      OverpassRequests.ZoomLevel -> viewport.zoom,
      OverpassRequests.QueryBody -> OverpassRequests.buildQuery(south, west, north, east)
    )
  }.toIndexedSeq.random  // .random makes Gatling pick a random city each iteration
}
