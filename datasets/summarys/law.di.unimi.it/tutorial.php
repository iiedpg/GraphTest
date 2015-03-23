<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
	<head> 
		<meta http-equiv="Content-Type" content="text/html;charset=utf-8"> 
		<style type="text/css">
			@import "css/content.css";
			@import "css/layout.css";
			{}
		</style>
		<script type="text/javascript">

		  var _gaq = _gaq || [];
		  _gaq.push(['_setAccount', 'UA-19887129-1']);
		  _gaq.push(['_trackPageview']);

		  (function() {
		    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
		    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
		    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
		  })();

		</script>
		<title>Laboratory for Web Algorithmics</title> 
	</head> 
	<body>
		<div id=header>
			<table style="border: none; padding: 0; margin: 0; border-spacing: 0; width: 100%">
				<tr style="border: none; padding: 0; margin: 0;">
					<td border=0 align=left style="border: none; padding: 0; margin: 0;"><img style="margin-left: 1em" alt="Laboratory for Web Algorithmics" src="img/header.png">
					<td border=0 align=right style="border: none; padding: 0; margin: 0;"><img style="float: right; margin-left: 1em" alt="Laboratory for Web Algorithmics" src="img/banner.png">
			</table>
		</div>

		<div id=menu>
			<ul id="menu-nav">
				<li><a href="index.php">Home<span class=arrow>&#10132;</span></a></li>
				<li><a href="publications.php">Publications<span class=arrow>&#10132;</span></a>
				<li><a href="software.php">Software<span class=arrow>&#10132;</span></a>
				<li><a href="datasets.php">Datasets<span class=arrow>&#10132;</span></a>
				<li><a href="citations.php">Citations<span class=arrow>&#10132;</span></a>
				<li><a href="collaborations.php">Collaborations<span class=arrow>&#10132;</span></a>
	      		</ul>
		</div>

		<div id="main">
			<div id="content">

<h1>Using the LAW datasets</h1>

<p>In this tutorial we illustrate the minimal steps required to download
and use a LAW dataset. Such a dataset is composed by a graph and, in some
cases, a list of node identifiers (e.g., URLs in the case of web
graphs).</p>

<p>We assume that you have correctly installed 
<a href="http://webgraph.dsi.unimi.it/">WebGraph</a>
and its dependencies. Please refer
to the <a href="http://webgraph.dsi.unimi.it/">WebGraph</a> site for further
details on installation.</p>

<p>We also assume that you add JVM-specific option so to allocate
enough memory: in case the JVM throws a
<code>java.lang.OutOfMemoryError</code>, you will probably
need to set a larger maximum Java heap size with the option
<samp>-Xmx</samp> (e.g., <samp>-Xmx2G</samp>; please use <samp>man java</samp>, or <samp>java
-X</samp>, for further help). </p>

<p>For sake of simplicity, the following examples will be based on
a Unix environment endowed with the usual utilities (as provided,
for example, by an up-to-date GNU/Linux distribution). The same
results can be obtained in any other environment supporting Java
and a reasonable set of standard utilities.</p>

<p>Every file in a dataset shares a common basename: in the
following discussion and examples we will use <samp><var>basename</var></samp>
to denote it.</p>

<h2>Structural information</h2>

<p>The structural part of a dataset is contained in the files
<samp><var>basename</var>.properties</samp> and <samp><var>basename</var>.graph</samp>.
These two files are sufficient to scan the graph sequentially.
If you want to access the graph randomly, we will need an
<em>offset file</em>, which can be easily generated starting from the
first two files.</p>

<p>First of all, download the files
<samp><var>basename</var>.properties</samp>,
<samp><var>basename</var>.graph</samp>,
and <samp><var>basename</var>.md5sums</samp>; since some files are very large,
please use a download tool that can restart the download in case of
failure (such as, for example,
<a href="http://curl.haxx.se/">cURL</a>, or
<a href="http://www.gnu.org/software/wget/wget.html">GNU/wget</a>).
Assuming that the data is located at
<samp>http://some.url/some/path/</samp>, the download can be
performed as</p>
<pre class="screen">
for ext in .properties .graph .md5sums; do
    wget -c http://some.url/some/path/<var>basename</var>$ext
done
</pre>

<p>Then, verify the MD5 sum of the downloaded file to check for
possible download problems:</p>
<pre class="screen">
md5sum -c <var>basename</var>.md5sums
</pre>

<p>Don't worry about <samp>can't open</samp> error messages (they
simply mean that the file <samp><var>basename</var>.md5sums</samp> contains
many MD5 sums for file you don't have already), but a <samp>MD5
check failed</samp> error message means that the related file is
corrupted and can't be used (you need to download it again, or to
rebuild it).</p>

<p><strong>Warning</strong>: On very large files, download errors
are not uncommon, as well as storage problems: a file that was once
correct can become corrupted. Please verify often file integrity
using MD5 sums.</p>

<!--<p>Now, uncompress <samp><var>basename</var>.graph.gz</samp> using
gzip and check the MD5 sum for the decompressed file</p>
<pre class="screen">
gunzip <var>basename</var>.graph.gz
md5sum -c <var>basename</var>.md5sums
</pre>
-->

<p>You can now build the offsets:</p>
<pre class="screen">
java it.unimi.dsi.webgraph.BVGraph -o -O -L <var>basename</var>
</pre>
<p>Actually, the command above will also create a file named
<samp><var>basename</var>.obl</samp> containing a serialised
<em>big list of offsets</em>. If WebGraph finds such a file, and its
modification date is later than that of the offsets file, it
will load (much more quickly) the former rather than the latter.</p>

<h3>Accessing a graph</h3>

<p>It is now trivial to load and access the graph. The class documentation
of <a href="http://webgraph.dsi.unimi.it/docs/it/unimi/dsi/webgraph/ImmutableGraph.html">ImmutableGraph</a>
explains in detail which methods are available. See also the 
<a href="http://webgraph.dsi.unimi.it/docs/it/unimi/dsi/webgraph/examples/package-summary.html">examples</a>.</p>

<h3>Other versions</h3>

<p>Note that each graph is available in different versions. As explained on the web site, we also provide
a <em>highly compressed version</em> (<samp>-hc</samp>) that is useful for transmission or for purely sequential
access (it turns to be actually <em>faster</em> for sequential access&mdash;smaller size,
better caching), and a <em>natural</em> version, in which the graph is presented in a &ldquo;natural&rdquo; way.
For web graphs, the natural presentation is in lexicographical URL ordering (i.e., nodes are numbered
following the lexicographical order of their URLs). For social networks, the same criterion is applied
when identifiers are available (e.g., names for DBLP): otherwise, we just give the graph in the form
it has been given to us from the source.</p>

<h3>Recompressing</h3>

<p>If you are interested in experimenting with different compression parameters,
you can easily recompress a graph.

<p>For instance, here we recompress a graph with a very low maximum backward
reference:</p>
<pre class="screen">
java it.unimi.dsi.webgraph.BVGraph -o -m 1 <var>basename</var>-hc <var>basename</var>-fast
</pre>
<p>The recompression will generate the files
<samp><var>basename</var>-fast.properties</samp>, <samp><var>basename</var>-fast.graph</samp>
and <samp><var>basename</var>-fast.offsets</samp>. You can generate
the usual big list with <code>java it.unimi.dsi.webgraph.BVGraph -o -L <samp><var>basename</var>-fast</code>.</p>


<h2>Mapping identifiers to nodes and viceversa</h2>

<p>In a LAW dataset graph nodes are simply represented by
natural numbers, but they correspond to actual entities (e.g., web pages) which
have an identifier (e.g., URLs). If you
are interested to map node numbers to identifiers and viceversa, you
need suitable data structures. These structures can be downloaded
in a pre-packaged form, but we will explain later how to rebuild them in case you
want to tune their parameters.</p>

<h3>Mapping nodes to identifiers</h3>

<p>The space occupied by the identifiers is usually very large&mdash;in most cases, significantly larger
than the associated graph. We store identifiers using <em><a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/FrontCodedStringList.html">front-coded lists</a></em>, which compress by
<em>prefix omission</em>. A string in a list is stored by noting the common prefix with the previous string,
and then by writing the rest of the string. Using sorted identifiers, the resulting compression is very high
(see the file <samp><var>basename</var>-nat.fcl</samp>). Since the recommended format is a permutation
of the natural order, we provide a sligtly bigger list <samp><var>basename</var>.fcl</samp>, which 
wraps a front-coded list over the sorted identifiers using a <a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/PermutedFrontCodedStringList.html">PermutedFrontCodedStringList</a>, and provide the correct answers for the permuted graph.

<h3>Mapping identifiers to nodes</h3>

<p>The situation is completely different for the inverse mappings, due to the availability of extremely
compact data structures that map strings to numbers. We provide a succinct function <samp><var>basename</var>.map</samp>
that
implements <a href="http://fastutil.dsi.unimi.it/docs/it/unimi/dsi/fastutil/objects/Object2LongFunction.html">Object2LongFunction</a>
and maps an identifier to the corresponding node, but will return <em>random numbers when applied to strings
that are not identifiers</em>, and a <em>signed</em> succinct function <samp><var>basename</var>.map</samp>
that implements <a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/StringMap.html">StringMap</a> (albeit not some optional operations)
and will be able to detect non-identifiers with high probability.

<h3>Mapping back and forth</h3>

<p>Finally, if you need to map back and forth we provide a
<a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/StringMap.html">StringMap</a> combining
a succinct function and a front-coded list, with name <samp><var>basename</var>.lmap</samp> (for &ldquo;literally signed map&rdquo;).
The two methods you need are 
<a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/StringMap.html#getLong(java.lang.Object)">getLong()</a>, which
provides mapping from strings to indices, and <a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/StringMap.html#list()">list()</a>,
which returns a list inverting the mapping.

<h2>Strongly connected components</h2>

<p>Each graph comes with files describing the structure of its strongly connected components. <samp><var>basename</var>.scc</samp>
is a sequence of integers in Java binary format assigning to each node its component (components are sorted by size, to component
0 is the largest component). Another file in the same format, <samp><var>basename</var>.sccsizes</samp>, indexed by components,
provides the size of each component. You can easily load these files using <samp>fastutil</samp>'s 
<a href="http://fastutil.dsi.unimi.it/docs/it/unimi/dsi/fastutil/io/BinIO.html">BinIO</a> facilities:
<pre class=screen>
int[] component = BinIO.loadInts( "<var>basename</var>.scc" );
int[] size = BinIO.loadInts( "<var>basename</var>.sccsizes" );
</pre>

<h2>Statistics</h2>

<p>For each graph, we provide a number of statistics. Some of them are already listed in the properties file associated with
the graph. Other (self-descriptive) statistics are contained in the <samp><var>basename</var>.stats</samp> file. The files
<samp><var>basename</var>.indegree</samp> and <samp><var>basename</var>.outdegree</samp> contain, in ASCII format, the indegree
and outdegree frequency distributions.

<p>In some cases, we provide the <em>average shortest path</em> and the <em>spid</em> of a graph. The first is a well-known measure
of closeness. The second (the <em>shortest-paths index of dispersion</em>) is computed as the variance-to-mean ratio of the
shortest-paths distribution. We have introduced this index in our paper about HyperANF, and we believe it is a very 
useful indicator, as under-dispersion (spid lesser than one) and over-dispersion (spid greater than one) distinguish
&ldquo;proper&rdquo; social networks from web graphs.

<p>Both numbers require a significant computational effort to be estimated
reliably (we use our new HyperANF algorithm to that purpose). We also
provide the standard deviation (the computation is probabilistic).

<h2>Rebuilding string maps</h2>

<p>All string-mapping structures are built starting from the
file <samp><var>basename</var>.urls</samp> (or <samp><var>basename</var>.ids</samp>), whose <var>i</var>-th line
contains the identifier of the <var>i</var>-th node (lines are numbered starting
from <strong>zero</strong>), or from the file
<samp><var>basename-nat</var>.urls</samp> (or
<samp><var>basename</var>-nat.ids</samp>), which contains (usually) the
identifiers in lexicographical order.

<p><strong>Warning:</strong> Usually URLs from our dataset are in ASCII, so you can use the <samp>-e</samp>
option on all classes to speed up reading. This is not true of identifiers, which are UTF-8 encoded.

<p>First of all, you need to download the files, uncompress them and check their MD5 (our example is a web graph):
<pre class="screen">
wget -c http://some.url/some/path/<var>basename</var>.urls.gz
wget -c http://some.url/some/path/<var>basename</var>-nat.urls.gz
gunzip <var>basename</var>.urls.gz
gunzip <var>basename</var>-nat.urls.gz
md5sum -c <var>basename</var>.md5sums
</pre>

<p>We build a succinct function mapping the URLs to their position using an
<a href="http://sux4j.dsi.unimi.it/docs/it/unimi/dsi/sux4j/mph/MWHCFunction.html"><code>MWHCFunction</code></a> provided by
<a href="http://sux4j.dsi.unimi.it/">Sux4J</a>:
<pre class="screen">
java it.unimi.dsi.sux4j.mph.MWHCFunction -z <var>basename</var>.map <var>basename</var>.urls.gz
</pre>

<p>Observe that if you want to be able not only to map URLs to node numbers but also to
determine if a given string is actually a URL present in the map, you will need a
<a href="http://sux4j.dsi.unimi.it/docs/it/unimi/dsi/util/StringMap.html"><code>StringMap</code></a>,
obtained, for instance, by <em>signing</em> the succinct function using a
<a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/ShiftAddXorSignedStringMap.html"><code>ShiftAddXorSignedStringMap</code></a>.
The signing process stores for each URL a hash (whose size you can control with an option) that will be used to check that
the result of the map is correct. The longer the hash, the more precise the map:
<pre class="screen">
java it.unimi.dsi.util.ShiftAddXorSignedStringMap -z <var>basename</var>.map <var>basename</var>.smap <var>basename</var>.urls.gz
</pre>

<p>Once you have created these files, you can use the map they
contain as in the folowing Java code snippet, which uses the
convenient
<a href="http://fastutil.dsi.unimi.it/docs/it/unimi/dsi/fastutil/io/BinIO.html#loadObject(java.lang.CharSequence)">
<code>loadObject()</code></a> method from
<a href="http://fastutil.dsi.unimi.it/docs/it/unimi/dsi/fastutil/io/BinIO.html">
<code>BinIO</code></a>, which opens a file
given its filename and retrieves the stored object:</p>
<pre class="screen">
...
String url;
Object2LongFunction&lt;? extends CharSequence> url2node = 
    (Object2LongFunction&lt;? extends CharSequence>) BinIO.loadObject( "<var>basename</var>.map" );
...
long node = url2node.getLong( url );
...
</pre>

where if <code>url</code> is a URL present in the map, then
<code>node</code> will be equal to the corresponding node number,
or 
<pre class="screen">
...
String url;
StringMap&lt;? extends CharSequence> url2node = 
    (StringMap&lt;? extends CharSequence>) BinIO.loadObject( "<var>basename</var>.smap" );
...
int node = url2node.get( url );
...
</pre>

where if <code>url</code> is a URL present in the map, then
<code>node</code> will be equal to the corresponding node number,
else <code>node</code> will be equal to -1. 

<p>To <em>map node numbers to URLs</em>, instead, you must
use a <a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/FrontCodedStringList.html">
<code>FrontCodedStringList</code></a>, available in the
<a href="http://dsiutils.dsi.unimi.it/">DSI utilities</a>. The process is slightly
tricky, as we need first to build a front-coded list for the <em>sorted</em> URLs,
and them wrap them in a <a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/PermutedFrontCodedStringList.html">PermutedFrontCodedStringList</a>.
<pre class="screen">
java it.unimi.dsi.util.FrontCodedStringList \
  -z -u -r 32 <var>basename</var>-nat.fcl &lt; <var>basename</var>-nat.urls.gz
</pre>
<p>This command needs a few comments. The option <samp>-r</samp> sets the <em>ratio</em>, that is, how often
a complete string (without prefix omission) will be stored. The largest the value, the smaller and slower the map.
The option <samp>-u</samp> compacts significantly the map because it stores the strings internally as UTF-8 bytes (also
in this case the map is slightly slower, but not that much).

<p>You will need now to download the
permutation <samp><var>basename</var>-nat2llpa.perm</samp>. Then,
<pre class="screen">
java it.unimi.dsi.util.PermutedFrontCodedStringList -i <var>basename</var>-nat.fcl \
       <var>basename</var>-nat2llpa.perm <var>basename</var>.fcl
</pre>

<p>Once you have created the file, you can use the map it contains
as in the following Java code snippet</p>
<pre class="screen">
...
CharSequence url;
List&lt;? extends CharSequence> node2url = (List&lt;? extends CharSequence>) BinIO.loadObject( "<var>basename</var>.fcl" );
...
url = node2url.get( node );
...
</pre>

<p>Finally, if you want to generate a <a
href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/LiterallySignedStringMap.html">literally
signed map</a> you have to combine the front-coded list with a map:
<pre class="screen">
java it.unimi.dsi.util.LiterallySignedStringMap -z <var>basename</var>.map <var>basename</var>.fcl <var>basename</var>.smap 
</pre>



<h3>Smaller but slower maps</h3>

<p>A front-coded list can be very large: 
if you do not have enough memory, or you can afford a slower access, you can build
an <em><a href="http://dsiutils.dsi.unimi.it/docs/it/unimi/dsi/util/ImmutableExternalPrefixMap.html">external prefix map</a></em> instead, which provides a bidirectional mapping
from URLs to nodes, and also from URL prefixes to intervals of nodes:</p>
<pre class="screen">
java it.unimi.dsi.util.ImmutableExternalPrefixMap \
  -z -b4Ki -o <var>basename</var>.urls.gz <var>basename</var>.pmap
</pre>


			</div>
    		</div>

		<div id="right">
			<fieldset><legend>People</legend>
				<ul>
					<li><a href="http://boldi.dsi.unimi.it/">Paolo Boldi</a>
					<li><a href="mailto:marino@di.unimi.it">Andrea Marino</a>
					<li><a href="mailto:monti@di.unimi.it">Corrado Monti</a>
					<li><a href="http://santini.dsi.unimi.it/">Massimo Santini</a>
					<li><a href="http://vigna.dsi.unimi.it/">Sebastiano Vigna</a>
				</ul>
		   	</fieldset>

			<fieldset><legend>Institutions</legend>
				<ul>
					<li><a href="http://dsi.unimi.it/">Dipartimento di Scienze dell'Informazione</a>
					<li><a href="http://www.unimi.it/">Università degli Studi di Milano</a>
				</ul>	
			</fieldset>
			<!--
			<fieldset><legend>Technology</legend>
				<ul>
					<li><a href="http://linux.org/">Linux</a>
					<li><a href="http://java.sun.com/">Java&trade;</a>
				</ul>	
			</fieldset>
			-->
		</div>

	</body> 
</html>
