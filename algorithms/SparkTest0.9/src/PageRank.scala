import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.graphx.PartitionStrategy._
/*
"Usage: PageRank <master> <edge_list_file>\n" +
  "    [--numIter= ]\n" +
  "        The number of iteration +
  "    [--output=<output_file>]\n" +
  "        If specified, the file to write the ranks to.\n" +
  "    [--numEPart=<num_edge_partitions>]\n" +
  "        The number of partitions for the graph's edge RDD. Default is 4.\n" +
  "    [--partStrategy=RandomVertexCut | EdgePartition1D | EdgePartition2D | " +
  "CanonicalRandomVertexCut]\n" +
  "        The way edges are assigned to edge partitions. Default is RandomVertexCut.")
*/
object PageRank extends Logging {
  def main(args: Array[String]) = {
    val master = args(0)
    val fname = args(1)
    val options =  args.drop(2).map { arg =>
      arg.dropWhile(_ == '-').split('=') match {
        case Array(opt, v) => (opt -> v)
        case _ => throw new IllegalArgumentException("Invalid argument: " + arg)
      }
    }
    def pickPartitioner(v: String): PartitionStrategy = {
      // TODO: Use reflection rather than listing all the partitioning strategies here.
      v match {
        case "RandomVertexCut" => RandomVertexCut
        case "EdgePartition1D" => EdgePartition1D
        case "EdgePartition2D" => EdgePartition2D
        case "CanonicalRandomVertexCut" => CanonicalRandomVertexCut
        case _ => throw new IllegalArgumentException("Invalid PartitionStrategy: " + v)
      }
    }
    val conf = new SparkConf()
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", "org.apache.spark.graphx.GraphKryoRegistrator")

    var numIter = 10
    var outFname = ""
    var numEPart = 4
    var partitionStrategy: Option[PartitionStrategy] = None

    options.foreach{
      case ("numIter", v) => numIter = v.toInt
      case ("output", v) => outFname = v
      case ("numEPart", v) => numEPart = v.toInt
      case ("partStrategy", v) => partitionStrategy = Some(pickPartitioner(v))
      case (opt, _) => throw new IllegalArgumentException("Invalid option: " + opt)
    }

    println("======================================")
    println("|             PageRank               |")
    println("======================================")

    val sc = new SparkContext(master, "PageRank(" + fname + ")", conf)
    val unpartitionedGraph = GraphLoader.edgeListFile(sc, fname,
      minEdgePartitions = numEPart).cache()
    val graph = partitionStrategy.foldLeft(unpartitionedGraph)(_.partitionBy(_))

    println("GRAPHX: Number of vertices " + graph.vertices.count)
    println("GRAPHX: Number of edges " + graph.edges.count)

    val pr = graph.staticPageRank(numIter).vertices.cache()
    // 问题1：这里如何对PageRank值做排序？即对(vertexId, pagerankVal)的记录集， 按照pagerankVal大小排序
    // 直接用下面的代码，好像不起作用
    //val pr = graph.staticPageRank(numIter).vertices.cache().sortByKey(false)
    
    val sortedPr = graph.staticPageRank(numIter).vertices.cache.map(p => {(p._2, p._1)}).sortByKey(false)
    
    
    //println("GRAPHX: Total rank: " + pr.map(_._2).reduce(_+_))
    if (!outFname.isEmpty) {
      logWarning("Saving pageranks of pages to " + outFname)
      // 问题2：这里如何将pr中的结果输出到文件中？用下面注释掉的代码，运行会报错
      //pr.map{case (id, r) => id + "\t" + r}.saveAsTextFile(outFname)
      // 用下面的方式可以输出，但不是 vertexId + "\t" + pagerankVal 的格式。
      //pr.saveAsTextFile(outFname)
      //outFname还是写成绝对路径吧
      sortedPr.map(p => {(p._2, p._1)}).saveAsTextFile(outFname)

    }
    sc.stop()
  }
}