package abr.teleop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ColorBlobDetector {
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    public Scalar mColorRadius = new Scalar(25,50,50,0);

    private Mat mSpectrum = new Mat();

    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    private double maxArea;	//made this an accessible variable
    private double momentX;	//added
    private double momentY;	//added
    private double centerX;	//added
    private double centerY;	//added
    private double innermostPoint;
    private double innermostPoint_adjR;
    private double innermostPoint_adjL;
    private double leftRoadBorder;
    private double rightRoadBorder;
    private boolean inGrass = false;

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        //Log.i("opencv",""+hsvColor.val[0]+","+hsvColor.val[1]+","+hsvColor.val[2]);

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);

    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        centerX = (double)mDilatedMask.cols()/2;
        centerY = (double)mDilatedMask.rows()/2;

        //Log.i("opencv", "Width="+mDilatedMask.cols()+",Length="+mDilatedMask.rows());

        innermostPoint = calculateInnermostPoint(mDilatedMask);
        calculateBorders(mDilatedMask);

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        maxArea = 0;	//added
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea){
                maxArea = area;
                calculateMoment(wrapper);	//calculate the moment of the biggest blob and innermost point
            }
        }
        //reset moment back to zero
        if(contours.size()==0){
            momentX = Integer.MAX_VALUE;
            momentY = Integer.MAX_VALUE;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }

    //Added
    public double getMaxArea(){
        return maxArea;
    }

    //Added - calculates center of mass of blob with largest area
    public void calculateMoment(MatOfPoint contour){
        List<Point> contourPoints = contour.toList();
        momentX = 0;
        momentY = 0;
        for(int i = 0; i < contourPoints.size(); i++){
            momentX += contourPoints.get(i).x;
            momentY += contourPoints.get(i).y;
        }
        momentX = momentX/contourPoints.size();
        momentY = momentY/contourPoints.size();

        momentX = momentX-centerX;
        momentY = momentY-centerY;
    }
    //also calculates inGrass
    public double calculateInnermostPoint(Mat mask){
        double rc;
        double impoint = Double.MAX_VALUE;
        double impoint_adjR = Double.MAX_VALUE;
        double impoint_adjL = Double.MAX_VALUE;
        ArrayList<Integer> points = new ArrayList<Integer>();
        for(int i = 0; i < (int)mask.cols(); i++){
            if(mask.get((2*mask.rows())/3, i) != null && mask.get((2*mask.rows())/3, i)[0] > 0){
                points.add(i);
            }
        }
        //Calculate innermost point
        if(!points.isEmpty()){
            for(int point:points){
                if(Math.abs(point-centerX)<Math.abs(impoint-centerX))
                    impoint = point;
            }
            rc = (double)impoint-centerX;
        } else {
            rc = Double.MAX_VALUE;
        }
        //Calculate innermost point, adjusted to the right
        double centerR = (double)mDilatedMask.cols()/3;
        if(!points.isEmpty()){
            for(int point:points){
                if(Math.abs(point-centerR)<Math.abs(impoint_adjR-centerR))
                    impoint_adjR = point;
            }
            innermostPoint_adjR = (double)impoint_adjR-centerR;
        } else {
            innermostPoint_adjR = Double.MAX_VALUE;
        }
        //Calculate innermost point adjusted to the left
        double centerL = (double)mDilatedMask.cols()*2/3;
        if(!points.isEmpty()){
            for(int point:points){
                if(Math.abs(point-centerL)<Math.abs(impoint_adjL-centerL))
                    impoint_adjL = point;
            }
            innermostPoint_adjL = (double)impoint_adjL-centerL;
        } else {
            innermostPoint_adjL = Double.MAX_VALUE;
        }
        //Calculate inGrass boolean
        if(points.size() == mask.cols())
            inGrass = true;
        else
            inGrass = false;
        return rc;
    }

    public void calculateBorders(Mat mask){
        leftRoadBorder = Double.MIN_VALUE;
        rightRoadBorder = Double.MAX_VALUE;

        ArrayList<Boolean> segmentClass = new ArrayList<Boolean>();
        ArrayList<Double> segmentBorders = new ArrayList<Double>();

        // Perform segmentation
        int rowOfInterestInd = 2*mask.rows()/3;
        boolean currClass = false;
        int longestRoadLen = 0;
        int roadLen = 0;

        String output = "";
        if(mask.get(rowOfInterestInd, 0) != null){
            if(mask.get(rowOfInterestInd, 0)[0] > 0){ //grass
                segmentClass.add(false);
                currClass = false;
                output = output + "0";
            }
            else{ //road
                segmentClass.add(true);
                currClass = true;
                output = output + "1";
                roadLen = 1;
                if(roadLen > longestRoadLen)
                    longestRoadLen = roadLen;
            }
            segmentBorders.add(0.0);
        }
        for(int i = 1; i < mask.cols(); i++){
            if(mask.get(rowOfInterestInd, i) != null){
                if(mask.get(rowOfInterestInd, i)[0] > 0){ //grass
                    if(currClass == true){
                        segmentClass.add(false);
                        segmentBorders.add((double) i);
                        currClass = false;
                    }
                    output = output + "0";
                } else { //road
                    if(currClass == false){
                        segmentClass.add(true);
                        segmentBorders.add((double) i);
                        currClass = true;
                        roadLen = 1;
                    } else {
                        roadLen++;
                    }
                    output = output + "1";
                    if(roadLen > longestRoadLen)
                        longestRoadLen = roadLen;
                }
            }
        }
        segmentBorders.add((double)mask.cols());

        // set borders to borders of longest road segment
        for(int i = 0; i < segmentClass.size(); i++){
            double len = segmentBorders.get(i+1) - segmentBorders.get(i);
            Log.i("opencv","len:"+len);
            if(segmentClass.get(i) == true && (Math.abs(segmentBorders.get(i+1) - segmentBorders.get(i)) == longestRoadLen)){
                leftRoadBorder = segmentBorders.get(i);
                rightRoadBorder = segmentBorders.get(i+1);
                Log.i("opencv","Longest Road Seg:"+i);
                break;
            }
        }

        centerX = (double)mDilatedMask.cols()/2;
        if(leftRoadBorder != Double.MIN_VALUE)
            leftRoadBorder = leftRoadBorder - centerX;
        if(rightRoadBorder != Double.MAX_VALUE)
            rightRoadBorder = rightRoadBorder - centerX;

        //Log.i("opencv","Num segments:"+segmentClass.size());
        //Log.i("opencv","Num borders:"+segmentBorders.size());
        //Log.i("opencv","Left:"+leftRoadBorder);
        //Log.i("opencv","Right:"+rightRoadBorder);
        //Log.i("opencv","Longest Road Seg Len:"+ longestRoadLen);
        //Log.i("opencv",output);
    }

    public double getLeftRoadBorder(){
        return leftRoadBorder;
    }

    public double getRightRoadBorder(){
        return rightRoadBorder;
    }

    public double getInnermostPoint(){
        return innermostPoint;
    }

    public double getInnermostPointL(){
        return innermostPoint_adjL;
    }

    public double getInnermostPointR(){
        return innermostPoint_adjR;
    }

    public boolean inGrass(){
        return inGrass;
    }

    public double getMomentX(){
        return momentX;
    }

    public double getMomentY(){
        return momentY;
    }

    public double getCenterX(){
        return centerX;
    }

    public double getCenterY(){
        return centerY;
    }

}
