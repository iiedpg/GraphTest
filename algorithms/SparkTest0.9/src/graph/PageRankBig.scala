package graph

import org.apache.spark.SparkContext
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import scala.collection.mutable.ArrayBuffer
import scala.math._
import common.Configuration._
import org.apache.spark.Partitioner
import org.apache.spark.HashPartitioner

object PageRankBig {
  val RANDOM_RATIO = 0.2
  val LAST_RATIO = 1 - RANDOM_RATIO
  val TURNS = 4 
  
  def main(args: Array[String]) {
    initialize()
    
    var inputPath = args(0)
    var outputPath = args(1)
    var nr_partition = 6
    if(args.length >= 3){
      nr_partition = args(2).toInt
    }

	var max_turns = TURNS
	if(args.length >= 4){
	  max_turns = args(3).toInt
	}

    println("input = " + inputPath + ",output = " + outputPath + "part = " + nr_partition + ", turns = " + max_turns)
    
    var sc = new SparkContext(SPARK_MASTER, "PageRank",
      SPARK_PATH, SPARK_JARS)

    var input = sc.textFile(hdfsPath(inputPath), nr_partition)
    val count = input.count
    val randomWeigth = RANDOM_RATIO / count

	var partitioner = new HashPartitioner(nr_partition) 
	println("has = " + partitioner);

    var start = System.currentTimeMillis()
    
    //1. Transform input into (a => b, weigth)
    var flatedInput = input.flatMap((line : String) =>{
      val parts = line.split(":")
      val from = parts(0).toInt
      var result = new ArrayBuffer[(Int, (Int, Double))]()
      
      for(i <- 1 until parts.length){
        var p = parts(i).split(",")
        result += ((from, (p(0).toInt, parts.length - 1)))
      }
      result
    }).partitionBy(partitioner).cache
    
    flatedInput.setName("flatedInput")
    
    //Create sliced datasets for each part of nodes
    var prDataset = input.map[(Int, Double)]((line : String) => {
      val parts = line.split(":")
      val from = parts(0).toInt
      (from, 1.0/count)
    }).partitionBy(partitioner).cache
    prDataset.setName("pr");
    
	var oldPr:RDD[(Int, Double)] = null
	var updateDataset : RDD[(Int,Double)] = null
	var lastUpdate : RDD[(Int, Double)] = null
	var cont = true
	var i = 1

	while(i <= max_turns && cont){
      val start = System.currentTimeMillis
	  //val startRddId = sc.lastRddId
      
      if(updateDataset != null){
        oldPr = prDataset
    	prDataset = prDataset.leftOuterJoin(updateDataset).mapValues(p => {
          p._2.getOrElse(0.0) + RANDOM_RATIO * 1.0 / count
        }).cache
        prDataset.setName("pr")
      }
      
      var currentDataset = flatedInput.join(prDataset)
      currentDataset.setName("v_e");
      
      lastUpdate = updateDataset
      updateDataset = currentDataset.map(p => 
        (p._2._1._1, p._2._2 / p._2._1._2 * LAST_RATIO)
        ).reduceByKey(partitioner, _ + _).cache
      updateDataset.setName("message")
      
      var messCount = updateDataset.count	  
      val end = System.currentTimeMillis
      println("turn =" + i + ", time = " + (end - start) + "\n\n")
	  //val endRddId = sc.lastRddId
	  //println("turn=" + i + "," + startRddId + "," + endRddId)
      
      //as with GraphX, we clean all temporary variables here
      if(lastUpdate != null){
    	lastUpdate.unpersist(blocking = false)
      }
      
      if(oldPr != null){
        oldPr.unpersist(blocking=false)
      }
      
      if(messCount <= 0){
        cont = false
      }
      
      i += 1
    }  
    
    prDataset = prDataset.sortByKey(true, 1)
    prDataset.saveAsTextFile(hdfsPath(outputPath))
    
    var end = System.currentTimeMillis()
    print("job time = " + (end - start))
  }
}