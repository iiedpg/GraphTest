package dummy

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.collection.mutable.ArrayBuffer
import scala.math._
import common.Configuration._

object WorldCount {
  def main(args: Array[String]) {
    initialize()
    
    var inputPath = args(0)
    var outputPath = args(1)
    var nr_partition = 6
    if(args.length >= 3){
      nr_partition = args(2).toInt
    }
    
    var sc = new SparkContext(SPARK_MASTER, "BFS", SPARK_PATH, SPARK_JARS)
    var mapped = sc.textFile(hdfsPath(inputPath), nr_partition).flatMap(line => {
      var result = ArrayBuffer[(String, Int)]()
      var parts = line.split("\\s+").foreach(p => {
        result += ((p, 1))
      })
      result
    }).cache
    var reduced = mapped.reduceByKey(_ + _)
    reduced.saveAsTextFile(hdfsPath(outputPath))
  }
}