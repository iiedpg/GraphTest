package common

object Configuration {
  //val SPARK_MASTER = "spark://192.168.10.185:7077"
  //val SPARK_PATH = "/home/bsp/spark"
  //val SPARK_JARS = List("/mnt/sda4/home/bsp/test/test.jar")
  val SPARK_MASTER = "local"
  //var SPARK_MASTER = "spark://127.0.0.1:37373"
  val SPARK_PATH = "/home/owner-pc/env/spark"
  val SPARK_JARS = List("/home/owner-pc/mount/E/workspace_lab/spark_submit/test.jar")

  def initialize() : Unit = {
    System.setProperty("spark.driver.host", "127.0.0.1")
  }
  
  def reduceFunc(x :Double, y : Double):Double = {
      (x, y) match{
	    case(-1, -1) => -1
	    case(-1, a) => a
	    case(a, -1) => a
	    case(a, b) => if(a < b) a else b
	  }
    }
  
  def isMinThan(x : Double, y : Double) = {
    if(x < 0){
      false
    }
    else if(y < 0){
      true
    }
    else{
      x < y
    }
  }
  
  /*def hdfsPath(path : String) = {
    "hdfs://192.168.10.185:9000" + path
  }*/
  def hdfsPath(path : String) = {
    path
  }
  val ROOT_ID = 1; 
  
  def main(args: Array[String]) {
    println(isMinThan(2.3, -1))
  }
} 
