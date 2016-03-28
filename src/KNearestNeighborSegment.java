import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

/**
 * This class is used to segment images
 * Created by rmbitiru on 3/8/16.
 */
public class KNearestNeighborSegment {

    public int[] colors ;
    public final int NO_OF_CLUSTERS = 5 ; // select a random k value
    public String inputImagePath = "";
    public String outputImagePath = "";
    public Point[] points ;

    private final int CLUSTER_DIFF_THRESHHOLD = 50;

    /**
     * Loads in an image
     * Usage: java KNearestNeighborSegment <inputFilePath> <outputFilePath> <segmentationTechnique [threshold | kmeans]>
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            KNearestNeighborSegment knns = new KNearestNeighborSegment();

            // check arguments
            if (args.length < 3) {
                knns.printUsage();
                System.exit(0);
            }

            // validate segmentation technique
            SegmentationTechnique segmentationTechnique = knns.extractSegmentationTechnique(args[2]) ;

            // generate colors
            knns.colors = knns.generateColors(knns.NO_OF_CLUSTERS) ;

            // load image
            knns.inputImagePath = knns.extractInputImagePath(args);
            knns.outputImagePath = knns.extractOutputImagePath(args);
            BufferedImage inputImage = knns.loadInputImage(knns.inputImagePath);
            String fileType = knns.getFileType(knns.inputImagePath);

            // convert image to gray scale
            BufferedImage grayScale = knns.convertToGrayScale(inputImage);

            // write the gray scale
            knns.writeImageToFile(grayScale, knns.outputImagePath + "[grayscale]", fileType);

            // apply K-Nearest neighbor segmentation
            BufferedImage knnsImage = knns.segment(grayScale, segmentationTechnique);

            // write segmented image
            knns.writeImageToFile(knnsImage, knns.outputImagePath, fileType);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SegmentationTechnique extractSegmentationTechnique(String str) {
        if (str.trim().toLowerCase().equals("threshhold"))
            return SegmentationTechnique.ThreshHold ;
        else if (str.trim().toLowerCase().equals("kmeans"))
            return SegmentationTechnique.KMeans ;
        else
            throw new IllegalArgumentException("Invalid Segmentation technique \"" + str + "\" specified") ;
    }

    public void printUsage() {
        System.out.println("Incorrect Usage. Use the command \"java KNearestNeighborSegment <pathToInputFile> " +
                "<pathToOutputFile> <segmentationTechnique [threshold | kmeans]>\"");
    }

    public int[] generateColors(int noOfClusters) {
        int[] colors = new int[noOfClusters] ;

        for (int i = 0; i < noOfClusters; i++) {
            Random rand = new Random() ;
            int red = rand.nextInt((255 - 0) + 1) + 0;
            int green = rand.nextInt((255 - 0) + 1) + 0;
            int blue = rand.nextInt((255 - 0) + 1) + 0;

            int color = new Color(red, green, blue).getRGB() ;
            colors[i] = color ;
        }

        return colors ;
    }

    public String extractInputImagePath(String[] args) {
        String inputFilePath = ".";
        if (null != args && args.length > 0)
            inputFilePath = args[0];
        return inputFilePath;
    }

    public String extractOutputImagePath(String[] args) {
        String outputFilePath = "";
        if (null != args && args.length > 1)
            outputFilePath = args[1];
        return outputFilePath;
    }

    public BufferedImage loadInputImage(String inputFilePath)
            throws IOException {
        BufferedImage image = null;
        File file = new File(inputFilePath);
        if (file.exists()) {
            image = ImageIO.read(file);
        } else {
            throw new FileNotFoundException(inputFilePath + " not found!");
        }
        return image;
    }

    // The choice on the selection of gray scale method is
    // selected from Luminosity method as specified in
    // http://www.johndcook.com/blog/2009/08/24/algorithms-convert-color-grayscale/
    public BufferedImage convertToGrayScale(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        // loop through, implement gray scale transformation
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color currColor = new Color(image.getRGB(j, i));
                int red = currColor.getRed();
                int green = currColor.getGreen();
                int blue = currColor.getBlue();

                // get averaged value based on the average method
                int averageColor = (red + green + blue) / 3;

                // create the new color
                Color grayScaleColor = new Color(averageColor, averageColor, averageColor);
                image.setRGB(j, i, grayScaleColor.getRGB());
            }
        }
        return image;
    }

    private void writeImageToFile(BufferedImage segmentedImage, String outputImagePath, String fileType)
            throws IOException {
        Scanner scanner = new Scanner(System.in);
        String outputFilePath = outputImagePath;
        File output = new File(outputImagePath);
        if (output.exists()) {
            System.out.println("Output file " + outputImagePath + " exists. Overwrite? [y/n]");
            if (scanner.nextLine().toLowerCase().equals("n")) {
                outputFilePath = outputImagePath + (Math.random() * 10000 + 1);
                System.out.println("Image written to " + outputFilePath);
            }
        }
        ImageIO.write(segmentedImage, fileType, new File(outputFilePath));
    }

    public String getFileType(String inputImagePath)
            throws IOException {
        String fileType = "";
        ImageInputStream iis = ImageIO.createImageInputStream(new File(inputImagePath));
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);
        while (imageReaders.hasNext()) {
            ImageReader reader = (ImageReader) imageReaders.next();
            fileType = reader.getFormatName();
        }
        return fileType;
    }

    /**
     * This method performs K-means clustering to determine similar
     * pixels whose positions are then returned in a clusters object
     * @param image buffered image file
     * @return clusters of related pixel locations
     */
    public BufferedImage segment(BufferedImage image, SegmentationTechnique SegmentationTechnique) {

        BufferedImage resImage = null ;

        if (SegmentationTechnique == SegmentationTechnique.ThreshHold) {
            resImage = performSimpleThreshHolding(image);
        } else if (SegmentationTechnique == SegmentationTechnique.KMeans) {
            resImage = performKMeansClustering(image) ;
        }
        return resImage ;
    }

    private BufferedImage performKMeansClustering (BufferedImage grayscaleImage) {

        // Step 1: Obtain the pixel clusters
        points = clusterPoints(grayscaleImage, NO_OF_CLUSTERS) ;

        // Step 2: apply the pixel clusters to the image using simple thresholding technique
        // based on the number of clusters
        /*
        for (int pointsCounter = 0; pointsCounter < points.length; pointsCounter++) {

            System.out.println("Processing point " + pointsCounter + ": " + points[pointsCounter].toString());

            // get the color for all pixels in current cluster
            int color = colors[pointsCounter % colors.length] ;

            // for all pixels in the cluster, convert them to cluster color
            for (Point currPoint : points) {
                currPoint.setGrayScaleValue(color);
            }
        }
        */

        // Step 3: use the points to reconstruct the
        // buffered image
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        int currPointsCounter = 0;  // notes where the current point is

        // loop through, implement gray scale transformation
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                // get current point characteristics
                int pointClusterColor = getClusterColor(points[currPointsCounter++]) ;

                // set the RGB value for the curr pixel
                grayscaleImage.setRGB(j, i, pointClusterColor);
            }
        }

        // return the new gray scale image
        return grayscaleImage ;
    }

    private int getClusterColor(Point point)  {

        // get the point cluster
        int cluster = point.getCluster() ;

        // get the color for all pixels in current cluster
        int color = colors[cluster % colors.length] ;

        return color ;
    }

    public Point[] clusterPoints(BufferedImage grayscaleImage, int noOfClusters) {

        // Represent each pixel in the image with a vector
        //  •intensity
        //  •color
        //  •color and location
        //  •color, location, other stuff (texture)
        // Choose distance weights i.e. is color more important than location?
        // Apply k-means
        // Pixels belong to the segment corresponding to centers
        // Different representations yield different segmentations

        // K means algorithm implemented with weight 1 for color and location
        // grouping based on the gray scale value of the pixels to determine
        // the clusters

        // Implemented algorithm
        // Compute the mean of each cluster.
        // Compute the distance of each point from each cluster by computing its distance from the corresponding cluster mean. Assign each point to the cluster it is nearest to.
        // Iterate over the above two steps till the sum of squared within group errors cannot be lowered any more.

        // create a store for each of the points with their x & y coordinates
        // and their gray scale value
        Point[] points = new Point[grayscaleImage.getWidth() * grayscaleImage.getHeight()] ;
        int pointsCounter = 0 ;
        int noOfRounds = 0 ; // total number of rounds

        // get the image attributes
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();

        // Step 1: get all the pixel values and their locations
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                Color currColor = new Color(grayscaleImage.getRGB(j, i));
                int grayScaleValue = currColor.getRed();

                Point currPoint = new Point(j, i, grayScaleValue);
                points[pointsCounter++] = currPoint;
            }
        }

        // Step 2: Select a random set of points as the k means cluster centers
        int[] clusterCenters = new int[noOfClusters];
        for (int i = 0; i < noOfClusters; i++) {
            Random rand = new Random();
            int index = rand.nextInt(points.length + 1);
            clusterCenters[i] = points[index].getGrayScaleValue();
        }

        // stores the number of points per cluster
        int[] noOfPointsInCluster = new int[NO_OF_CLUSTERS] ;

        while (true) {

            // Step 3: loop through each of the pixels and find out
            // where their gray scale values are closest to (when compared
            // to the cluster centers gray scale values)
            // cluster all points to their closest center

            for (int pCounter = 0; pCounter < points.length; pCounter++) {

                int diff = Integer.MAX_VALUE;
                Point currPoint = points[pCounter];
                int pointCluster = -1 ;
                for (int cluster = 0; cluster < noOfClusters; cluster++) {

                    // find distance to cluster center's gray scale value
                    // calculate using euclidean distance, L2
                    int diffGrayScaleValue = (int) Math.sqrt(Math.pow(currPoint.getGrayScaleValue() - clusterCenters[cluster], 2));
                    if (diffGrayScaleValue < diff) {
                        diff = diffGrayScaleValue;
                        pointCluster = cluster ;
                    }
                }

                currPoint.setCluster(pointCluster); // add point to cloud
                noOfPointsInCluster[pointCluster]++; // add the number of points in class
            }

            // Step 3: Calculate the mean for each cluster
            int[] clusterSums = new int[noOfClusters];
            for (int cluster = 0; cluster < noOfClusters; cluster++) {
                for (int pCounter = 0; pCounter < points.length; pCounter++) {
                    Point currPoint = points[pCounter];
                    if (currPoint.getCluster() == cluster) {
                        clusterSums[cluster] += currPoint.getGrayScaleValue() ;
                    }
                }
            }

            int[] clusterMeans = new int[noOfClusters] ;
            for (int cluster = 0; cluster < noOfClusters; cluster++) {
                clusterMeans[cluster] = clusterSums[cluster] / noOfPointsInCluster[cluster] ;
            }

            // Step 4: find out if the cluster means are too large and if
            // so, repeat the steps in calculating the cluster means until the
            // cluster difference values converge
            boolean performOtherKMeansRound = false;
            for (int clusterMeansCounter = 0; clusterMeansCounter < clusterSums.length; clusterMeansCounter++) {

                // perform the difference using euclidean distance
                double clusterError = Math.sqrt(Math.pow(clusterMeans[clusterMeansCounter] - clusterCenters[clusterMeansCounter], 2)) ;
                if (clusterError > CLUSTER_DIFF_THRESHHOLD) {
                    performOtherKMeansRound = true;
                    clusterCenters[clusterMeansCounter] = clusterMeans[clusterMeansCounter];
                    noOfRounds++ ;
                }
            }

            if (performOtherKMeansRound)
                continue;
            else
                break;
        }

        System.out.println("Total number of rounds: " + noOfRounds);
        return points ;
    }

    private BufferedImage performSimpleThreshHolding(BufferedImage grayscaleImage) {

        // set the threshold value
        int grayScaleThreshholdValue = 200 ;

        // set the two binary thresh-hold states
        int whiteRGB = new Color(255, 255, 255).getRGB() ;
        int blackRGB = new Color(0, 0, 0).getRGB();

        // Step 1: get all the points and their respective gray scale value
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();

        // loop through, implement gray scale transformation
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {

                Color currColor = new Color(grayscaleImage.getRGB(j, i));
                int grayScaleValue = currColor.getRed();

                if (grayScaleValue < grayScaleThreshholdValue)
                    grayscaleImage.setRGB(j, i, whiteRGB);
                else
                    grayscaleImage.setRGB(j, i, blackRGB);
            }
        }
        return grayscaleImage ;
    }

    class Cluster {

        ArrayList<Point> points = new ArrayList<>() ;

        public Cluster() {}

        void addPoint(Point point) {
            points.add(point);
        }
    }

    class Point {
        int x ;
        int y;
        int grayScaleValue ;
        int cluster;

        Point(int a, int b, int grayScaleValue) {
            x = a ;
            y = b ;
            this.grayScaleValue = grayScaleValue ;
        }

        int getX() {
            return x ;
        }

        int getY () {
            return y ;
        }

        int getGrayScaleValue() {
            return grayScaleValue ;
        }

        void setGrayScaleValue(int grayScaleValue) {
            this.grayScaleValue = grayScaleValue ;
        }

        int getCluster(){
            return cluster ;
        }

        void setCluster(int cluster) {
            this.cluster = cluster ;
        }

        public String toString() {
            return "x: " + x + " y: " + y + " cluster: " + cluster + " grayscale value:" + grayScaleValue ;
        }
    }
}
