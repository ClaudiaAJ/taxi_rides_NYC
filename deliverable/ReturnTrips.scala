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

    ToDo: Add your implementation here

    trips
  }
}
