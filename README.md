Pattern Recognition Hackathon 
8.3.2016
Reagan Mbitiru <rmugo@andrew.cmu.edu> 

* Compiling the application *
To compile the application, use the command:
 
<pre>
javac KNearestNeighborSegment.java
</pre> 

* Running the application * 
To run the application, use the command:
<pre>
java KNearestNeighborSegment <inputFilePath> <outputFilePath> <segmentationTechnique [threshold | kmeans]>
<pre>

The application creates a gray scale image and a segmented image. The segmented image can be generated using either the threshhold technique or using kmeans
