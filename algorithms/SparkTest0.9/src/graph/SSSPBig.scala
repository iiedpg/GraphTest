package graph

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._
import scala.collection.mutable.ArrayBuffer
import scala.math._
import common.Configuration._
import org.apache.spark.Partitioner
import org.apache.spark.HashPartitioner

object SSSPBig {  
  def main(args: Array[String]) {
    initialize()
    
    var inputPath = args(0)
    var outputPath = args(1)
    var nr_partition = 6
    if(args.length >= 3){
      nr_partition = args(2).toInt
    }

	var max_turns = 0
	if(args.length >= 4){
	  max_turns = args(3).toInt
	}

    println("input = " + inputPath + ",output = " + outputPath + "part = " + nr_partition + ", turns = " + max_turns)
    
    var sc = new SparkContext(SPARK_MASTER, "SSSP",
      SPARK_PATH, SPARK_JARS)
    
    
    
    var input = sc.textFile(hdfsPath(inputPath), nr_partition)

	var partitioner = new HashPartitioner(nr_partition) 
	println("has = " + partitioner);
    
    var start = System.currentTimeMillis;
    
    //1. Transform input into (a => b, weigth)
    var flatedInput = input.flatMap((line : String) =>{
      val parts = line.split(":")
      val from = parts(0).toInt
      var result = new ArrayBuffer[(Int, (Int, Double))]()
      
      for(i <- 1 until parts.length){
        var p = parts(i).split(",")
        result += ((from, (p(0).toInt, p(1).toDouble)))
      }
      result
    }).partitionBy(partitioner).cache
    
    flatedInput.setName("flatedInput")
    
    //Create sliced datasets for each part of nodes
    var prDataset = input.map[(Int, (Double, Double, Int))]((line : String) => {
      val parts = line.split(":")
      val from = parts(0).toInt
      
      if(from == ROOT_ID){
        (from, (-1, 0, -1))
      }
      else{
    	(from, (-1, -1, -1))
      }
    }).partitionBy(partitioner).cache
    
    prDataset.setName("pr");
	
    //val accum = sc.accumulator(0)
    //var cont = true
	
	
	var oldPr:RDD[(Int, (Double, Double, Int))] = null
	var updateDataset : RDD[(Int, (Int, Double))] = null
	var lastUpdate : RDD[(Int, (Int, Double))] = null
	var i = 1
	var cont = true
	
    while(cont){
      val start = System.currentTimeMillis
      
      if(updateDataset != null){
        oldPr = prDataset
        prDataset = prDataset.leftOuterJoin(updateDataset).mapValues(p => {
	        val updateValue:(Int, Double) = p._2.getOrElse((-1, -1))
	        val currentValue = p._1._2
	        val parent = p._1._3
	        
	        //println("update = " + updateValue + ",current = " + currentValue + ", parent = " + parent)
	        if(isMinThan(updateValue._2, currentValue)){
	          //update
	          //println("updated")          
	          (currentValue, updateValue._2, updateValue._1)
	        }
	        else{
	          (currentValue, currentValue, parent)
	        }
	        
	        //p._2.getOrElse(0.0) + RANDOM_RATIO * 1.0 / count
	      }).cache
	    prDataset.setName("pr")
      }
      
      var currentDataset = flatedInput.join(prDataset)
      //currentDataset.saveAsTextFile("/tmp/spark/current_" + i)
      currentDataset.setName("v_e")
      
      lastUpdate = updateDataset
      updateDataset = currentDataset.filter(p => {
        (i == 1 & p._2._2._2 >= 0) || isMinThan(p._2._2._2, p._2._2._1)
      }).map(p =>{
        (p._2._1._1, (p._1, p._2._2._2 + p._2._1._2))
      }).reduceByKey(partitioner, (a, b) => {
        if(isMinThan(b._2, a._2)){
          (b._1, b._2)
        }        
        else{
          (a._1, a._2)
        }
      }).cache
      updateDataset.setName("message")
      //updateDataset.saveAsTextFile("/tmp/spark/update_" + i)

      //prDataset_pre.saveAsTextFile("/tmp/spark/pr_pre_" + i)      

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
   
    println("total rounds = " + i)
    
    //prDataset = prDataset.sortByKey(true, 1)
    prDataset.saveAsTextFile(hdfsPath(outputPath))
    val end = System.currentTimeMillis
    print("job time = " + (end - start))
  }
}