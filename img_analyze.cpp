#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <opencv2/highgui/highgui.hpp>

using namespace std;
using namespace cv; 

int thresh = 100;
int max_thresh = 255;
RNG rng(12345);

float pixel_val_avg (Mat image, Point center, int radius) //function to calculate average pixel intensity, given radii and centers of circle
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
	Mat img=imread("image072915B.jpeg");//replace by the name of image
	namedWindow("input",CV_WINDOW_NORMAL);
	//imshow("input",img);

	Mat img_smooth;
	//namedWindow("img_smooth",CV_WINDOW_NORMAL);
	GaussianBlur(img,img_smooth,Size(3,3),0,0);//remove noise
	//imshow("img_smooth",img_smooth);
   
    Mat output;
    cv::inRange(img_smooth, cv::Scalar(0,0,200), cv::Scalar(200, 200, 255), output);//rgb based thresholding

Mat canny_output;
  vector<vector<Point> > contours;
  vector<Vec4i> hierarchy;
 
 //removing salt and pepper noise
 medianBlur ( output,output, 3 );
  namedWindow("final",CV_WINDOW_NORMAL);
  medianBlur( output,output, 15 );
 // imshow("final",output);
 
  //finding contours
  findContours( output, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );
  
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
      if(a>largest_area){ //only significant areas used
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

       if(a>=avg_area){ 
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
       }
       drawContours( drawing, contours, i, Scalar(0,0,0), 5, 8, hierarchy, 0, Point() );
     }
     // cout<<y_min<<" "<<y_max<<" "<<x_min<<" "<<x_max<<endl;
     avg_radius=avg_radius/k;
     // cout<<avg_radius<<endl;
     if(contours.size()!=9)
     {
            Scalar color = Scalar( 0,0,0);//rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );

        circle( img, Point(x_min,y_min), avg_radius, color, 5, 8, 0 ); //detecting 9th top-left circle
     }
  /// Show in a window
  center_final[k]=Point(x_min,y_min);
  radius_f[k]=avg_radius;
  //namedWindow( "Contours", CV_WINDOW_NORMAL );
 // imshow( "Contours", img );

  float array_intensities[9];
  for (int j=0;j<9;j++)
  {
  array_intensities[j]=pixel_val_avg(img,center_final[j],radius_f[j]/2);
  cout<<array_intensities[j]<<" "<<"center is "<<center_final[j].x<<" "<<center_final[j].y<<endl;
  //take radius/2, with same centers
}
// cout<<array_intensities<<endl;
  waitKey(0);
	cv::waitKey();
    return 0;
}

