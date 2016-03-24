package org.opencv.samples.imagemanipulations;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageManipulationsActivity extends Activity {
    private final static int RESULT_LOAD_IMG = 1;
    public float C1_intensity,C2_intensity,C3_intensity,C4_intensity,C5_intensity,C6_intensity,QC1_intensity,QC2_intensity,sample_intensity;
    public float QC1_conc_pred, QC2_conc_pred, sample_conc_pred;
    public double C1_conc=0, C2_conc=62.5, C3_conc=125, C4_conc=250, C5_conc=500,C6_conc=1000;
    public double QC1_conc=156, QC2_conc=750;
    private String selectedImagePath;
    private static final String  TAG                 = "OCVSample::Activity";
    private ImageView img;
    BaseLoaderCallback mOpenCVCallBack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.image_manipulations_surface_view);
        BaseLoaderCallback  mOpenCVCallBack = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
                buttonLoadImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_PICK);
                        i.setType("image/*");
                        i.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(i, "Select Picture"), RESULT_LOAD_IMG);
//                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

//                startActivityForResult(i, RESULT_LOAD_IMG);
                    }
                });
            }
        };
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mOpenCVCallBack))
            Log.e("Error", "Failed to load OpenCV");

    }

    public float pixel_val_avg(Mat image, Point center, int radius) {
        {
            float pixel_val_avg = 0;
            int num_points = 0;
            Size sizeRgba = image.size();
            int rows = (int) sizeRgba.height;
            int cols = (int) sizeRgba.width;

            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++) {
                    double dist = Math.sqrt((c - center.x) * (c - center.x) + (r - center.y) * (r - center.y));
                    if (dist < radius) {
                        double[] data = image.get(r, c);
                        pixel_val_avg += (data[2] + data[1] + data[0]) / 3;
                        num_points++;
                    }
                }
            return pixel_val_avg / num_points;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data)
            {
                // Get the Image from data

                Uri selectedImage = data.getData();

                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);


                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                cursor.moveToFirst();
                String picturePath = cursor.getString(columnIndex);

                cursor.close();
                System.out.println("Image Path : " + picturePath);
//                img.setImageURI(selectedImage);
                ImageView imageView = (ImageView) findViewById(R.id.imgView);
                imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));


                Mat img= Imgcodecs.imread(picturePath);
                Size sizeRgba = img.size();
                Mat img_smooth=new Mat();
                org.opencv.core.Size s = new Size(3,3);
                Imgproc.GaussianBlur(img, img_smooth, s, 0);

                int rows = (int) sizeRgba.height;
                int cols = (int) sizeRgba.width;

                Mat output= new Mat();
                Mat img_gray= new Mat();
                Mat canny_output=new Mat(rows,cols, CvType.CV_8UC1);


                Core.inRange(img_smooth, new Scalar(0, 0, 200), new Scalar(200, 200, 255), output);
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
// /storage/emulated/0/Download/image072915B.jpeg
                Mat output1=new Mat();

                Imgproc.medianBlur(output, output1, 5);
               Imgproc.medianBlur(output1, output, 5);
                Imgproc.medianBlur(output, output1, 5);
                Imgproc.medianBlur(output1, output, 5);

                Imgproc.findContours(output,contours,new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                int contour_count=contours.size();
                Point[] center      = new Point[contour_count];
                float[] radius     = new float[contour_count];
                float[] radius1 =new float[contour_count];

                double avg_area=0;
                double avg_radius=0;
                double largest_area=0;
                int largest_contour_index=0;
                double x_max, y_max, x_min, y_min;

                for( int i = 0; i< contours.size(); i++ )
                {
                    Mat contour = contours.get(i);
                    double a= Imgproc.contourArea(contour, false);  //  Find the area of contour
                    avg_area+=a;
                    if(a>largest_area){
                        largest_area=a;
                        largest_contour_index=i;
                    }
                    MatOfPoint contour_1= contours.get(i);
                    Point p = new Point();
//                    float rad=0;
                    Imgproc.minEnclosingCircle( new MatOfPoint2f( contour_1.toArray()), p, radius);
                    center[i] = p;
                    radius1[i]=radius[0];
                }
//        Log.("center",center[largest_contour_index].x);
                avg_area=avg_area/contours.size();
//                Imgcodecs.imwrite("output",output);
                Point p = center[largest_contour_index];
                x_min= center[largest_contour_index].x;
                x_max=x_min;
                y_min=(int) center[largest_contour_index].y;
                y_max=y_min;

                int k=0;

                Point[] center_final = new Point[9];
                float[] radius_f     = new float[9];

                for(int i=0; i<contours.size();i++)
                {
                    Scalar color = new Scalar(0,255,0);
                    Mat contour = contours.get(i);
                    double a=Imgproc.contourArea( contour,false);
                    // cout<<a<<" "<<avg_area<<endl;

                    if(a>=avg_area){
                        // cout<<radius[i]<<" "<<center[i].x<<","<<center[i].y<<endl;
                        Imgproc.circle( img, center[i], (int)radius1[i], color, 2, 8, 0 );
                        avg_radius+=(int)radius1[i];
                        radius_f[k]=radius1[i];
                        center_final[k]=center[i];
                        k++;
                        if((int)center[i].x>x_max)
                            x_max=(int) center[i].x;
                        if((int)center[i].x<x_min)
                            x_min=(int) center[i].x;
                        if((int) center[i].y<y_min)
                            y_min=(int) center[i].y;
                        if((int)center[i].y>y_max)
                            y_max=(int) center[i].y;
                    }
                    //drawContours( drawing, contours, i, color, 2, 8, hierarchy, 0, Point() );
                }
                avg_radius=avg_radius/k;
                // cout<<avg_radius<<endl;
                if(contours.size()!=9)
                {
                    Scalar color = new Scalar(0,255,0 );
                    Imgproc.circle(img, new Point(x_min, y_min), (int) avg_radius, color, 2, 8, 0);
                }
                center_final[k]=new Point(x_min,y_min);
                radius_f[k]=(float) avg_radius;

                float array_intensities[]=new float[9];
                for (int j=0;j<9;j++)
                {
                    array_intensities[j]=pixel_val_avg(img,center_final[j],(int) radius_f[j]/2);
                    //take radius/2, with same centers
                }
                //closest C1
                double val=1000;
                 for (int n=0;n<9;n++)
                {
                     if(val>((x_min-center_final[n].x)*(x_min-center_final[n].x)+(y_min-center_final[n].y)*(y_min-center_final[n].y)))
                     {
                         val=((x_min-center_final[n].x)*(x_min-center_final[n].x)+(y_min-center_final[n].y)*(y_min-center_final[n].y));
                         C1_intensity=array_intensities[n];
                }
            }
                //closest C2
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((x_min-center_final[n].x)*(x_min-center_final[n].x)+(0.5*(y_min+y_max)-center_final[n].y)*(0.5*(y_min+y_max)-center_final[n].y)))
                    {
                        val=((x_min-center_final[n].x)*(x_min-center_final[n].x)+(0.5*(y_min+y_max)-center_final[n].y)*(0.5*(y_min+y_max)-center_final[n].y));
                         C2_intensity=array_intensities[n];
                    }
                }
                //closest C3
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((x_min-center_final[n].x)*(x_min-center_final[n].x)+(y_max-center_final[n].y)*(y_max-center_final[n].y)))
                    {
                        val=((x_min-center_final[n].x)*(x_min-center_final[n].x)+(y_max-center_final[n].y)*(y_max-center_final[n].y));
                        C3_intensity=array_intensities[n];
                    }
                }
                //closest C4
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((x_max-center_final[n].x)*(x_max-center_final[n].x)+(y_max-center_final[n].y)*(y_max-center_final[n].y)))
                    {
                        val=((x_max-center_final[n].x)*(x_max-center_final[n].x)+(y_max-center_final[n].y)*(y_max-center_final[n].y));
                        C4_intensity=array_intensities[n];
                    }
                }
                //closest C5
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((x_max-center_final[n].x)*(x_max-center_final[n].x)+(0.5*(y_min+y_max)-center_final[n].y)*(0.5*(y_min+y_max)-center_final[n].y)))
                    {
                        val=((x_max-center_final[n].x)*(x_max-center_final[n].x)+(0.5*(y_min+y_max)-center_final[n].y)*(0.5*(y_min+y_max)-center_final[n].y));
                        C5_intensity=array_intensities[n];
                    }
                }
                //closest C6
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((x_max-center_final[n].x)*(x_max-center_final[n].x)+(y_min-center_final[n].y)*(y_min-center_final[n].y)))
                    {
                        val=((x_max-center_final[n].x)*(x_max-center_final[n].x)+(y_min-center_final[n].y)*(y_min-center_final[n].y));
                        C6_intensity=array_intensities[n];
                    }
                }
                //closest QC1
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((0.5*(x_min+x_max)-center_final[n].x)*(0.5*(x_min+x_max)-center_final[n].x)+(y_max-center_final[n].y)*(y_max-center_final[n].y)))
                    {
                        val=((0.5*(x_min+x_max)-center_final[n].x)*(0.5*(x_min+x_max)-center_final[n].x)+(y_max-center_final[n].y)*(y_max-center_final[n].y));
                        QC1_intensity=array_intensities[n];
                    }
                }
                //closest QC2
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((0.5*(x_min+x_max)-center_final[n].x)*(0.5*(x_min+x_max)-center_final[n].x)+(y_min-center_final[n].y)*(y_min-center_final[n].y)))
                    {
                        val=((0.5*(x_min+x_max)-center_final[n].x)*(0.5*(x_min+x_max)-center_final[n].x)+(y_min-center_final[n].y)*(y_min-center_final[n].y));
                        QC2_intensity=array_intensities[n];
                    }
                }
                //closest sample
                val=1000;
                for (int n=0;n<9;n++)
                {
                    if(val>((0.5*(x_min+x_max)-center_final[n].x)*(0.5*(x_min+x_max)-center_final[n].x)+(0.5*(y_min+y_max)-center_final[n].y)*(0.5*(y_min+y_max)-center_final[n].y)))
                    {
                        val=((0.5*(x_min+x_max)-center_final[n].x)*(0.5*(x_min+x_max)-center_final[n].x)+(0.5*(y_min+y_max)-center_final[n].y)*(0.5*(y_min+y_max)-center_final[n].y));
                        sample_intensity=array_intensities[n];
                    }
                }

            }

        //simple linear regression
        double[] x=new double[5];double[] y=new double[5];
        int N=5;
//        x[0]=Math.log10(C1_conc);
        x[1]=Math.log10(C2_conc);x[2]=Math.log10(C3_conc);
        x[3]=Math.log10(C4_conc);x[4]=Math.log10(C5_conc);
        x[0]=Math.log10(C6_conc);
//        x[5]=Math.log10(C6_conc);

//        y[0]=C1_intensity;
        y[0]=C6_intensity;
        y[1]=C2_intensity;y[2]=C3_intensity;y[3]=C4_intensity;y[4]=C5_intensity;//y[5]=C6_intensity;
        // first pass
        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
        for (int i = 0; i < N; i++)
            sumx  += x[i];
        for (int i = 0; i < N; i++)
            sumx2 += x[i]*x[i];
        for (int i = 0; i < N; i++)
            sumy  += y[i];
        double xbar = sumx / N;
        double ybar = sumy / N;
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < N; i++) {
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            yybar += (y[i] - ybar) * (y[i] - ybar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }
        double slope  = xybar / xxbar;
        double intercept = ybar - slope * xbar;
        double rss = 0.0;      // residual sum of squares
        double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < N; i++) {
            double fit = slope*x[i] + intercept;
            rss += (fit - y[i]) * (fit - y[i]);
            ssr += (fit - ybar) * (fit - ybar);
        }
        float val1=(float)(QC1_intensity-intercept)/(float)slope;
        QC1_conc_pred= (float) Math.pow(10,val1);

        float val2=(float)(QC2_intensity-intercept)/(float)slope;
        QC2_conc_pred= (float) Math.pow(10,val2);

        float val3=(float)(sample_intensity-intercept)/(float)slope;
        sample_conc_pred=(float) Math.pow(10,val3);

//    }
//
//    public void sendMessage(View v){

        TextView textView1 = (TextView) findViewById(R.id.textView1);
        textView1.setText("Intensities");

        TextView textView2 = (TextView) findViewById(R.id.textView2);
        String s="C1:"+C1_intensity+", C2:"+C2_intensity+", C3:"+C3_intensity+", C4:"+C4_intensity+", C5:"+C5_intensity+", C6:"+C6_intensity;
        textView2.setText(s);

        TextView textView3 = (TextView) findViewById(R.id.textView3);
        textView3.setText("Concentration predictions");

        TextView textView4 = (TextView) findViewById(R.id.textView4);
        String s1="QC1:"+QC1_conc_pred+", QC2:"+QC2_conc_pred;
        textView4.setText(s1);

        TextView textView5 = (TextView) findViewById(R.id.textView5);
        textView5.setText("Concentration Prediction Error");

        TextView textView6 = (TextView) findViewById(R.id.textView6);
        String s2="QC1 error:"+Math.abs((QC1_conc_pred-QC1_conc)/QC1_conc)*100+"%                                      QC2 error:"+Math.abs((QC2_conc_pred-QC2_conc)/QC2_conc)*100+"%";
        textView6.setText(s2);

        TextView textView7 = (TextView) findViewById(R.id.textView7);
        String s3="Sample Concentration:"+sample_conc_pred;
        textView7.setText(s3);
    }
    }



