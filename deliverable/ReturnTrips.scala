import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Column
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row

// (c) 2021 Thomas Neumann, Timo Kersten, Alexander Beischl, Maximilian Reif

object ReturnTrips {
  def compute(trips : Dataset[Row], dist : Double, spark : SparkSession) : Dataset[Row] = {

    import spark.implicits._

    //ToDo: Add your implementation here

    // set relevant values:
    val radius = 6371000 // 6371km = 6371000m
    val max_time_difference = 60*60*8  // 60sec * 60min = 1h, max difference between rides is set to be 8 hours
    val lat_in_degree = dist / 111139 // 111.139km = 111139m

    // function given for distance calculation
    val makeDistExpr = (lat1: Column, lon1: Column, lat2: Column, lon2: Column) => {
      val dLat = toRadians(abs(lat2 - lat1))
      val dLon = toRadians(abs(lon2 - lon1))
      val hav = pow(sin(dLat * 0.5), 2) + pow(sin(dLon * 0.5), 2) * cos(toRadians(lat1)) * cos(toRadians(lat2))
      abs(lit(radius * 2) * asin(sqrt(hav)))
    }

    // turn datetime into unique number with unix and get relevant columns
    val trips_unix = trips
      .withColumn("unix_pickup_timestamp", unix_timestamp($"tpep_pickup_datetime"))
      .withColumn("unix_dropoff_timestamp", unix_timestamp($"tpep_dropoff_datetime"))
      .select($"pickup_longitude", $"pickup_latitude", $"dropoff_longitude", $"dropoff_latitude",
        $"unix_pickup_timestamp", $"unix_dropoff_timestamp")


    // solve task analogously to exercise 6 from sheet 8: create buckets and join on buckets first
    val trips_unixBuck = trips_unix
      .withColumn("pickup_latBuck", floor($"pickup_latitude" / lat_in_degree))
      .withColumn("dropoff_latBuck", floor($"dropoff_latitude" / lat_in_degree))
      .withColumn("pickup_timeBuck", floor($"unix_pickup_timestamp" / max_time_difference))
      .withColumn("dropoff_timeBuck", floor($"unix_dropoff_timestamp" / max_time_difference))
      .sort($"pickup_timeBuck", $"dropoff_timeBuck")
      .cache()

    // use "explode" to flatten the array
    val trips_unixBuckNeighbors = trips_unixBuck
      .withColumn("pickup_latBuck",
        explode(array($"pickup_latBuck" - 1, $"pickup_latBuck", $"pickup_latBuck" + 1)))
      .withColumn("dropoff_latBuck",
        explode(array($"dropoff_latBuck" - 1, $"dropoff_latBuck", $"dropoff_latBuck" + 1)))
      .withColumn("pickup_timeBuck",
        explode(array($"pickup_timeBuck"-1, $"pickup_timeBuck")))

    // join on buckets
    val joined = trips_unixBuck.as("a").join(trips_unixBuckNeighbors.as("b"),
      ($"a.dropoff_timeBuck" === $"b.pickup_timeBuck") &&
        ($"a.pickup_latBuck" === $"b.dropoff_latBuck") &&
        ($"a.dropoff_latBuck" === $"b.pickup_latBuck") &&
        // pseudo sql query:
        (makeDistExpr($"a.dropoff_latitude", $"a.dropoff_longitude", $"b.pickup_latitude", $"b.pickup_longitude") < dist) &&
        (makeDistExpr($"b.dropoff_latitude", $"b.dropoff_longitude", $"a.pickup_latitude", $"a.pickup_longitude") < dist) &&
        ($"a.unix_dropoff_timestamp" + max_time_difference > $"b.unix_pickup_timestamp") &&
        ($"a.unix_dropoff_timestamp" < $"b.unix_pickup_timestamp")
    )

    joined
  }
}
