package dummy

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.collection.mutable.ArrayBuffer
import scala.math._
import common.Configuration._
import org.apache.spark.Partitioner
import org.apache.spark.HashPartitioner

object ReduceTest {
  def main(args: Array[String]) {
    initialize()
    
    var inputPath = args(0)
    var outputPath = args(1)
    var nr_partition = 4
    if(args.length >= 3){
      nr_partition = args(2).toInt
    }
    
    var partitioner = new HashPartitioner(4);
        
    var sc = new SparkContext(SPARK_MASTER, "Reduce", SPARK_PATH, SPARK_JARS)
    var input = sc.textFile(hdfsPath(inputPath), nr_partition).map(line => {
      var parts = line.split("\\s+")
      (parts(0).toInt, parts(1).toInt)
    })

    var input2 = sc.textFile(hdfsPath(inputPath), nr_partition).map(line => {
      var parts = line.split("\\s+")
      (parts(0).toInt, parts(1).toInt + 1)
    })


    var ds3 = input.join(input2)
    var ds4 = ds3.map(kv => (kv._1, kv._2._1 + kv._2._2))
    var reduced = ds4.reduceByKey(_ + _)
    reduced.saveAsTextFile(hdfsPath(outputPath))
  }
}