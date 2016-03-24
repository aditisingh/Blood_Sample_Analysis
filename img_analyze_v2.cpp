#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <algorithm>
#include <cmath>
#include <opencv2/highgui/highgui.hpp>

using namespace std;
using namespace cv; 

float C1_conc=0, C2_conc=62.5, C3_conc=125, C4_conc=250, C5_conc=500, C6_conc=1000; // all in ng/ml
float QC1_conc=156, QC2_conc=750; //in ng/ml
int thresh = 100;
int max_thresh = 255;
RNG rng(12345);

float pixel_val_avg (Mat image, Point center, int radius)
{
float pixel_val_avg=0; int num_points=0;
for ( int r = 0; r < image.rows; r++ )
    for ( int c = 0; c < image.cols; c++ )
    {
        double dist = sqrt ( ( c - center.x ) * ( c - center.x ) + ( r - center.y ) * ( r  - center.y ));
        if ( dist < radius )
            {
              pixel_val_avg+=(image.at<Vec3b>(r,c)[2]+image.at<Vec3b>(r,c)[1]+image.at<Vec3b>(r,c)[0])/3;
              num_points++;
            }
    }
return pixel_val_avg/num_points;
}

int main( int argc, char** argv )
{
	Mat img=imread("image072915B.jpeg");//cropped.jpeg")
	namedWindow("input",CV_WINDOW_NORMAL);
	imshow("input",img);

	Mat img_smooth;
	// namedWindow("img_smooth",CV_WINDOW_NORMAL);
	GaussianBlur(img,img_smooth,Size(3,3),0,0);
	imshow("img_smooth",img_smooth);
   
    Mat output;
  cvtColor(img_smooth,img_smooth,CV_BGR2HSV);
   Mat low_red_hue_thresh; Mat high_red_hue_thresh;
   cv::inRange(img_smooth,cv::Scalar(0,50,50),cv::Scalar(10,255,255),low_red_hue_thresh); 
   cv::inRange(img_smooth,cv::Scalar(160,50,50),cv::Scalar(179,255,255),high_red_hue_thresh);

    // Mat output;
    addWeighted(low_red_hue_thresh,1.0,high_red_hue_thresh,1.0,0.0,output);

    // cv::inRange(img_smooth, cv::Scalar(0,0,200), cv::Scalar(200, 200, 255), output);
namedWindow("inRange",CV_WINDOW_NORMAL);
imshow("inRange",output);

  vector<vector<Point> > contours;
  vector<Vec4i> hierarchy;

 
  medianBlur ( output,output, 3 );
  namedWindow("salt-pepper-removal",CV_WINDOW_NORMAL);
  imshow("salt-pepper-removal",output);
  /// Find contours
  // Mat img_final;
  namedWindow("more medianBlur",CV_WINDOW_NORMAL);
  medianBlur( output,output, 15 );
  // dilate(output, img_final, 0, Point(-1, -1), 2, 1, 1);
  imshow("more medianBlur",output);

 Rect bounding_rect;
  findContours( output, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );

  /// Draw contours
  
  vector<Point2f>center( contours.size() );
  // vector<vector<Point> > contours_poly( contours.size() );

  vector<float>radius( contours.size() );
  Mat drawing = Mat::zeros( output.size(), CV_8UC3 );
  double avg_area=0;
  double avg_radius=0; 
  int x_lt=0, y_lt=0;
  double largest_area=0;
  int largest_contour_index=0;
  int x_max, y_max, x_min, y_min;
  for( int i = 0; i< contours.size(); i++ )
     {
       double a=contourArea( contours[i],false);  //  Find the area of contour
       avg_area+=a;
      if(a>largest_area){
       largest_area=a;
       largest_contour_index=i; 
     }
       minEnclosingCircle( (Mat)contours[i], center[i], radius[i] );
     }
     avg_area=avg_area/contours.size();
     x_min=x_max=center[largest_contour_index].x;
     y_min=y_max=center[largest_contour_index].y;
     int k=0;
     vector<Point2f>center_final(9);
     double radius_f[9];

     for(int i=0; i<contours.size();i++)
     {
      Scalar color = Scalar( 0,0,0);//rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );
      double a=contourArea( contours[i],false); 
       // cout<<a<<" "<<avg_area<<endl;

       // if(a>=avg_area){ 
        // cout<<radius[i]<<" "<<center[i].x<<","<<center[i].y<<endl;
        circle( img, center[i], (int)radius[i], color, 5, 8, 0 );
        avg_radius+=(int)radius[i];
        radius_f[k]=radius[i];
        center_final[k]=center[i];
        k++;
        if(center[i].x>x_max)
          x_max=center[i].x;
        if(center[i].x<x_min)
          x_min=center[i].x;
        if(center[i].y<y_min)
          y_min=center[i].y;
        if(center[i].y>y_max)
          y_max=center[i].y;
       // }
       drawContours( drawing, contours, i, Scalar(0,0,0), 5, 8, hierarchy, 0, Point() );
     }
     // cout<<y_min<<" "<<y_max<<" "<<x_min<<" "<<x_max<<endl;
     avg_radius=avg_radius/k;
     // cout<<avg_radius<<endl;
     if(contours.size()!=9)
     {
            Scalar color = Scalar( 0,0,0);//rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );

        circle( img, Point(x_min,y_min), avg_radius, color, 5, 8, 0 );
     }
  /// Show in a window
  center_final[k]=Point(x_min,y_min);
  radius_f[k]=avg_radius;
  namedWindow( "Contours", CV_WINDOW_NORMAL );
  imshow( "Contours", img );
  float x_final[9],y_final[9];
  for(int j=0;j<9;j++)
  {
    x_final[j]=center_final[j].x;
    y_final[j]=center_final[j].y;
  }
  sort(x_final,x_final+9);
  sort(y_final,y_final+9);
  // for(int j=0;j<9;j++)
    // cout<<x_final[j]<<" "<<y_final[j]<<endl;
  int x_l,x_m,x_r,y_l,y_m,y_r;

  x_l= (x_final[0]+x_final[1]+x_final[2])/3;
  x_m= (x_final[3]+x_final[4]+x_final[5])/3;
  x_r= (x_final[6]+x_final[7]+x_final[8])/3;

  y_l= (y_final[0]+y_final[1]+y_final[2])/3;
  y_m= (y_final[3]+y_final[4]+y_final[5])/3;
  y_r= (y_final[6]+y_final[7]+y_final[8])/3;

center_final[0]=Point(x_l,y_l);//c1
center_final[1]=Point(x_l,y_m);//c2
center_final[2]=Point(x_l,y_r);//c3
center_final[3]=Point(x_m,y_l);//qc2
center_final[4]=Point(x_m,y_m);//sample
center_final[5]=Point(x_m,y_r);//qc1
center_final[6]=Point(x_r,y_l);//c6
center_final[7]=Point(x_r,y_m);//c5
center_final[8]=Point(x_r,y_r);//c4


  float array_intensities[9];
  for (int j=0;j<9;j++)
  {
  array_intensities[j]=pixel_val_avg(img,center_final[j],avg_radius/2);
  //cout<<array_intensities[j]<<" "<<"center is "<<center_final[j].x<<" "<<center_final[j].y<<endl;
  //take radius/2, with same centers
}
// cout<<array_intensities<<endl;
int N=5;

float x[5],y[5];
x[0]=log10(C2_conc),x[1]=log10(C3_conc),x[2]=log10(C4_conc);
x[3]=log10(C5_conc),x[4]=log10(C6_conc);

y[0]=array_intensities[1],y[1]=array_intensities[2],y[2]=array_intensities[8];
y[3]=array_intensities[7],y[4]=array_intensities[6];

float sum_x=0, sum_y=0,sum_xy=0,sum_x_2=0;
for(int i=0;i<N;i++)
{
  sum_x+=x[i];
  sum_y+=y[i];
  sum_xy+=x[i]*y[i];
  sum_x_2+=x[i]*x[i];
}

float slope=(N*sum_xy-sum_x*sum_y)/(N*sum_x_2-(sum_x*sum_x));
float intercept=(sum_x_2*sum_y-sum_x*sum_xy)/(N*sum_x_2-sum_x*sum_x);

float QC1_conc_pred=pow(10,(array_intensities[5]-intercept)/slope); 
float QC2_conc_pred=pow(10,(array_intensities[3]-intercept)/slope); 

int error1=100*abs(QC1_conc-QC1_conc_pred)/QC1_conc;
int error2=100*abs(QC2_conc-QC2_conc_pred)/QC2_conc;

cout<<"error in qc1: "<<error1<<"% "<<", error in qc2 : "<<error2<<"%"<<endl;

float sample_conc;

if(error2<=20)
{
  if(error1<=20)
  {
    cout<<"Satisfies the condition."<<endl;
    sample_conc=pow(10,(array_intensities[4]-intercept)/slope); 
    cout<<"sample concentration is "<<sample_conc<<" ng/ml."<<endl;
  }
}

waitKey(0);
	cv::waitKey();
    return 0;
}

