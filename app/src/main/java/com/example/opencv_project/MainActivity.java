package com.example.opencv_project;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera_permission();

        cameraBridgeViewBase=findViewById(R.id.cameraView);

        cameraBridgeViewBase.setCameraIndex(0);

        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {}

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

                Mat rgbaFrame = inputFrame.rgba();
                Mat gray = new Mat();
                gray = inputFrame.gray();
                Imgproc.GaussianBlur(gray, gray, new Size(7, 7), 0);

                Mat edged = new Mat();
                Imgproc.Canny(gray, edged, 50, 100);
                Imgproc.dilate(edged, edged, new Mat(), new Point(-1, -1), 1);
                Imgproc.erode(edged, edged, new Mat(), new Point(-1, -1), 1);

                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                if (contours.isEmpty()) {
                    return inputFrame.rgba();
                }

                // Sort contours by area (largest to smallest)

                Collections.sort(contours, new Comparator<MatOfPoint>() {
                    @Override
                    public int compare(MatOfPoint o1, MatOfPoint o2) {
                        return Double.compare(Imgproc.contourArea(o2), Imgproc.contourArea(o1));
                    }
                });

                double pixelsPerMetric = 1.0;

                Mat drawing = Mat.zeros(gray.size(), CvType.CV_8UC3);
                for (MatOfPoint cnt : contours) {
                    if (Imgproc.contourArea(cnt) < 100) {
                        continue;
                    }

                    // Minimum area rectangle
                    RotatedRect box = Imgproc.minAreaRect(new MatOfPoint2f(cnt.toArray()));

                    // Calculate the center points of the rectangle sides
                    Point[] rectPoints = new Point[4];
                    box.points(rectPoints);
                    Point tl = rectPoints[0];
                    Point tr = rectPoints[1];
                    Point br = rectPoints[2];
                    Point bl = rectPoints[3];

                    Point tltr = new Point((tl.x + tr.x) / 2, (tl.y + tr.y) / 2);
                    Point blbr = new Point((bl.x + br.x) / 2, (bl.y + br.y) / 2);

                    Point tlbl = new Point((tl.x + bl.x) / 2, (tl.y + bl.y) / 2);
                    Point trbr = new Point((tr.x + br.x) / 2, (tr.y + br.y) / 2);

                    // Compute the Euclidean distances between the midpoints
                    double dA = Math.sqrt(Math.pow(tltr.x - blbr.x, 2) + Math.pow(tltr.y - blbr.y, 2));
                    double dB = Math.sqrt(Math.pow(tlbl.x - trbr.x, 2) + Math.pow(tlbl.y - trbr.y, 2));

                    // Compute the size of the object
                    double dimA = dA / pixelsPerMetric;
                    double dimB = dB / pixelsPerMetric;

                    // Draw the contours and dimensions on the image
//                    Imgproc.drawContours(drawing, List.of(new MatOfPoint(rectPoints)), -1, new Scalar(0, 255, 0), 2);
                    List<MatOfPoint> contourList = new ArrayList<>();
                    contourList.add(new MatOfPoint(rectPoints));
                    Imgproc.drawContours(drawing, contourList, -1, new Scalar(0, 255, 0), 2);
                    Imgproc.putText(drawing, String.format("%.1f", dimA), new Point(tltr.x - 15, tltr.y - 10), Imgproc.FONT_HERSHEY_SIMPLEX, 0.65, new Scalar(255, 255, 255), 2);
                    Imgproc.putText(drawing, String.format("%.1f", dimB), new Point(trbr.x + 10, trbr.y), Imgproc.FONT_HERSHEY_SIMPLEX, 0.65, new Scalar(255, 255, 255), 2);
                }

                return drawing;
            }
        });

        if(OpenCVLoader.initDebug()){
            cameraBridgeViewBase.enableView();
        }
    }
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    void camera_permission() {
        if(checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length>0 && grantResults[0]!=PackageManager.PERMISSION_GRANTED){
            camera_permission();
        }
    }
}