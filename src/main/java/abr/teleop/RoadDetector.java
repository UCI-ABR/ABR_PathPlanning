package abr.teleop;

import android.graphics.Point;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
/*import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;*/
// import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xinyun Zou on 11/9/2016.
 */

public class RoadDetector {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private List<Point> mPathPts;
    /*private int[] mPathCs;
    private int[] mPathRs;*/
    private int centerX;	//added
    private int centerY;	//added
    private int momentX;	//added
    private int momentY;	//added
    private int pathDetected; //added

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat blurredImage = new Mat();
    Mat grayImage = new Mat();
    Mat contrastImage = new Mat();
    Mat gradientImage = new Mat();
    Mat binaryImage = new Mat();
    Mat mDilatedMask = new Mat();
    private Mat grayImageUpCopy = new Mat();
    private Mat contrastImageUpCopy = new Mat();
    private Mat gradientImageUpCopy = new Mat();
    private Mat binaryImageUpCopy = new Mat();
    private Mat mDilatedMaskUpCopy = new Mat();

    //int sampleRate = 4;
    //Size blurSize = new Size(3,3);
    //double alpha = 2.0, beta = -2.0;
    Mat xFirstDerivative = new Mat(), yFirstDerivative = new Mat();
    int ddepth = CvType.CV_16S;
    Mat absXD = new Mat(), absYD = new Mat();
    double maxVal = 255;
    //double thresh = 135; //155;
    //int dilateSize = 15; //20;
    Mat dilation_kernel = new Mat();
    int rowsNum, colsNum, rowsMid, colsMid;
    int[] startInd, endInd, PathCols, PathRows;
    int x = 0, y = 0;

    public void process(Mat rgbaImage, int sampleRate, Size blurSize, double alpha, double beta,
                        double thresh, int dilateSize) {

        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        if (sampleRate > 3) {
            for (int s = 1; s < (int) (Math.log(sampleRate) / Math.log(2)); s++) {
                Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
            }
        }
        /*Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        *//*Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);*/

        Imgproc.GaussianBlur(mPyrDownMat, blurredImage, blurSize, 0, 0);

        Imgproc.cvtColor(blurredImage, grayImage, Imgproc.COLOR_RGB2GRAY);

        grayImage.convertTo(contrastImage, -1, alpha, beta);

        Imgproc.Sobel(contrastImage, xFirstDerivative, ddepth, 1, 0);
        Imgproc.Sobel(contrastImage, yFirstDerivative, ddepth, 0, 1);

        Core.convertScaleAbs(xFirstDerivative, absXD);
        Core.convertScaleAbs(yFirstDerivative, absYD);
        Core.addWeighted(absXD, 0.5, absYD, 0.5, 0, gradientImage);

        Imgproc.threshold(gradientImage, binaryImage, thresh, maxVal, Imgproc.THRESH_BINARY);

        dilation_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
                new Size(2*dilateSize + 1, 2*dilateSize+1),
                new org.opencv.core.Point(dilateSize, dilateSize));
        mDilatedMask = new Mat();
        Imgproc.dilate(binaryImage, mDilatedMask, dilation_kernel);

        rowsNum = mDilatedMask.rows();
        colsNum = mDilatedMask.cols();

        rowsMid = rowsNum / 2;
        colsMid = colsNum / 2;
        startInd = new int[rowsNum];
        endInd = new int[rowsNum];
        PathCols = new int[rowsNum];
        PathRows = new int[rowsNum];

        for (int i = rowsNum - 2; i > (int)(0.7*rowsMid); i--) {
            int j = colsMid;
            if (PathCols[i+1] != 0) {
                j = PathCols[i+1];
            }
            int jPrev = j;

            if (mDilatedMask.get(i,j)[0] == (double) 255) continue;

            startInd[i] = j;
            while (mDilatedMask.get(i,j)[0] == (double) 0 && j > 0) {
                startInd[i] = j;
                j = j - 1;
            }

            j = jPrev;
            endInd[i] = j;
            while (mDilatedMask.get(i,j)[0] == (double) 0 && j < colsNum - 1) {
                endInd[i] = j;
                j = j + 1;
            }

            PathCols[i] = (startInd[i] + endInd[i])/2;
        }

        for (int i = (int)(0.7*rowsMid) + 1; i < rowsNum; i++) {
            if (PathCols[i-1] != 0 && Math.abs(PathCols[i-1] - PathCols[i]) > 5) {
                int j = PathCols[i-1];
                if (mDilatedMask.get(i,j)[0] == (double) 255) {
                    PathCols[i] = 0;
                    startInd[i] = 0;
                    endInd[i] = 0;
                }
                else {
                    startInd[i] = j;
                    while (mDilatedMask.get(i,j)[0] == (double) 0 && j > 0) {
                        startInd[i] = j;
                        j = j - 1;
                    }
                    j = PathCols[i-1];
                    endInd[i] = j;
                    while (mDilatedMask.get(i,j)[0] == (double) 0 && j < colsNum - 1) {
                        endInd[i] = j;
                        j = j + 1;
                    }
                    PathCols[i] = (startInd[i] + endInd[i])/2;
                }
            }
            else if (PathCols[i-1] == 0 && i > (int)(1.3*rowsMid) && PathCols[i] != 0) {
                PathCols[i] = 0;
                startInd[i] = 0;
                endInd[i] = 0;
            }
        }

        // Resize paths to fit the original image size

        for (int i = 0; i < PathCols.length; i++) {
            PathCols[i] = PathCols[i] * sampleRate;
            PathRows[i] = i * sampleRate;
        }

        mPathPts = new ArrayList<>();
        for (int i = 0; i < PathCols.length; i++) {
            x = PathCols[i];
            y = PathRows[i];
            if (x > 0) { mPathPts.add(new Point(x, y)); }
        }

        /*mPathCs = new int[mPathPts.size()];
        mPathRs = new int[mPathPts.size()];
        for (int m = 0; m < mPathPts.size(); m++) {
            mPathCs[m] = mPathPts.get(m).x;
            mPathRs[m] = mPathPts.get(m).y;
        }*/

        centerX = rgbaImage.cols()/2;
        centerY = rgbaImage.rows()/2;

        if(mPathPts.size()==0){
            momentX = 0;
            momentY = 0;
        } else {
            calculateMoment(mPathPts);
        }

        pathDetected = mPathPts.size();

        Imgproc.pyrUp(grayImage, grayImageUpCopy);
        Imgproc.pyrUp(contrastImage, contrastImageUpCopy);
        Imgproc.pyrUp(gradientImage, gradientImageUpCopy);
        Imgproc.pyrUp(binaryImage, binaryImageUpCopy);
        Imgproc.pyrUp(mDilatedMask, mDilatedMaskUpCopy);
        if (sampleRate > 3) {
            for (int s = 1; s < (int) (Math.log(sampleRate) / Math.log(2)); s++) {
                Imgproc.pyrUp(grayImageUpCopy, grayImageUpCopy);
                Imgproc.pyrUp(contrastImageUpCopy, contrastImageUpCopy);
                Imgproc.pyrUp(gradientImageUpCopy, gradientImageUpCopy);
                Imgproc.pyrUp(binaryImageUpCopy, binaryImageUpCopy);
                Imgproc.pyrUp(mDilatedMaskUpCopy, mDilatedMaskUpCopy);
            }
        }
    }

    public List<Point> getPathPoints() { return mPathPts; }

    /*public int[] getPathCols() {
        return mPathCs;
    }

    public int[] getPathRows() {
        return mPathRs;
    }*/

    // Added
    public int pathDetected(){
        return pathDetected;
    }

    //Added - calculates center of mass of path
    public void calculateMoment(List<Point> mPathPts){
        momentX = 0;
        momentY = 0;
        for(int i = 0; i < mPathPts.size(); i++){
            momentX += mPathPts.get(i).x;
            momentY += mPathPts.get(i).y;
        }
        momentX = momentX/mPathPts.size();
        momentY = momentY/mPathPts.size();

        momentX = momentX-centerX;
        momentY = momentY-centerY;

        /*momentX = momentX / 16;
        momentY = momentY / 16;*/
    }

    //Added
    public int getCenterX(){
        return centerX;
    }

    //Added
    public int getCenterY(){
        return centerY;
    }

    //Added
    public int getMomentX(){
        return momentX;
    }

    //Added
    public int getMomentY(){
        return momentY;
    }

    //Added
    public Mat getGrayImage() { return grayImageUpCopy; }

    //Added
    public Mat getContrastImage() { return contrastImageUpCopy; }

    //Added
    public Mat getGradientImage() { return gradientImageUpCopy; }

    //Added
    public Mat getBinaryImage() { return binaryImageUpCopy; }

    //Added
    public Mat getDilatedMask() { return mDilatedMaskUpCopy; }
}
