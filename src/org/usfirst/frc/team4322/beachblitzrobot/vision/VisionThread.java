package org.usfirst.frc.team4322.beachblitzrobot.vision;

import java.util.ArrayList;

import org.usfirst.frc.team4322.beachblitzrobot.Robot;
import org.usfirst.frc.team4322.dashboard.DashboardInputField;
import org.usfirst.frc.team4322.logging.RobotLogger;

import com.ni.vision.*;
import com.ni.vision.NIVision.GetImageSizeResult;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import static com.ni.vision.NIVision.*;

public class VisionThread extends Thread
{
    private boolean abort = false;
    private boolean queried = false;
    private VisionReport out = null;
    private long rateLimit = 50;
    private static final double idealAspect = 20 / 12;
    ParticleFilterCriteria2 criteria[] = new ParticleFilterCriteria2[1];
    ParticleFilterOptions2 filterOptions = new ParticleFilterOptions2(0, 0, 1,
            1);
    @DashboardInputField(field = "Red Min Value: ")
    public static int rMin = 0;
    @DashboardInputField(field = "Red Max Value: ")
    public static int rMax = 255;
    @DashboardInputField(field = "Green Min Value: ")
    public static int gMin = 245;
    @DashboardInputField(field = "Green Max Value: ")
    public static int gMax = 255;
    @DashboardInputField(field = "Blue Min Value: ")
    public static int bMin = 0;
    @DashboardInputField(field = "Blue Max Value: ")
    public static int bMax = 255;
    @DashboardInputField(field = "Camera Vertical FOV: ")
    //logitech camera vFOV.
    public static double vFOV = 2*Math.atan(Math.tan(Math.toRadians(78/2))*Math.sin(Math.atan(9./16.)));
    @DashboardInputField(field = "Camera Horizontal FOV: ")
    //logitech camera vFOV.
    public static double hFOV = 51.f;

    Image frame = imaqCreateImage(ImageType.IMAGE_RGB, 0);
    Image binarizedFrame = imaqCreateImage(ImageType.IMAGE_U8, 0);
    Image display = imaqCreateImage(ImageType.IMAGE_RGB, 0);
    public static int id = -1;
    public static double targetHU1 = .525;

    public VisionThread()
    {
    }

    private double ratioToScore(double ratio)
    {
        return Math.max((double) 0,
                Math.min(100 * (1 - Math.abs(1 - ratio)), (double) 100.f));
    }

    public boolean queried()
    {
        return queried;
    }

    public void die()
    {
        abort = true;
    }

    public VisionReport getResult()
    {
        return out;
    }

    @Override
    public void run()
    {
        if (id == -1)
            id = IMAQdxOpenCamera("cam1",IMAQdxCameraControlMode.CameraControlModeController);
        	criteria[0] = new ParticleFilterCriteria2(
                MeasurementType.MT_AREA_BY_IMAGE_AREA, 0.25, 1.5, 0,
                0);

        while (!abort)
        {
            try
            {
            	IMAQdxStartAcquisition(id);
                IMAQdxGetImage(id, frame,
                        IMAQdxBufferNumberMode.BufferNumberModeBufferNumber, 0);
                IMAQdxStopAcquisition(id);
                GetImageSizeResult size = imaqGetImageSize(frame);
                imaqColorThreshold(binarizedFrame, frame, 255, ColorMode.RGB,
                        new Range(rMin, rMax), new Range(gMin, gMax),
                        new Range(bMin, bMax));
                imaqDuplicate(display, binarizedFrame);
                imaqParticleFilter4(binarizedFrame, binarizedFrame, criteria,
                        filterOptions, null);
                int numParticles = imaqCountParticles(binarizedFrame, 1);
                RobotLogger.getInstance().log("Particle count: %d\n",numParticles);
                ArrayList<VisionReport> objects = new ArrayList<>();
                for (int i = 0; i < numParticles; i++)
                {
                    VisionReport vr = new VisionReport();
                     
                    vr.area = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_AREA);
                    vr.bboxwidth = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_BOUNDING_RECT_WIDTH);
                    vr.bboxheight = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_BOUNDING_RECT_HEIGHT);
                    vr.bboxleft = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_BOUNDING_RECT_LEFT);
                    vr.bboxtop = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_BOUNDING_RECT_TOP);
                    vr.xpos = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_CENTER_OF_MASS_X);
                    vr.ypos = imaqMeasureParticle(binarizedFrame, i, 0,
                            MeasurementType.MT_CENTER_OF_MASS_Y);
                    vr.boundingBoxArea = vr.bboxheight * vr.bboxwidth;
                    vr.areaScore = ratioToScore(
                            (vr.area / (vr.boundingBoxArea)) * 3.0);

                    vr.aspect = ((double) vr.bboxwidth / vr.bboxheight);
                    vr.aspectScore = ratioToScore(
                            (vr.aspect / idealAspect) * (3.0 / 5.0));
                    vr.score = (vr.aspectScore + vr.areaScore) / 2;
                    vr.relxpos = vr.xpos / size.width;
                    vr.relypos = vr.ypos / size.height;
                    if(vr.ypos < size.height*.7)
                    {
                        vr.score += 10;
                    }
                    else
                    {
                        continue;
                    }
                    objects.add(vr);
                }
                objects.sort((x, y) -> x.score > y.score ? 1 : -1);
                for (VisionReport in : objects)
                {
                    RobotLogger.getInstance().log("SCORE: %f\n", in.score);
                    RobotLogger.getInstance().log(
                            "AREA SCORE: %f ASPECT SCORE: %f\n", in.areaScore,
                            in.aspectScore);
                    RobotLogger.getInstance().log("AREA: %f ASPECT: %f\n",
                            in.area, in.aspect);
                    RobotLogger.getInstance().log("XPOS: %f YPOS: %f\n",
                            in.xpos, in.ypos);
                    RobotLogger.getInstance().log("XPOS-REL: %f YPOS-REL: %f\n",
                            in.relxpos, in.relypos);
//                    RobotLogger.getInstance().log("DISTANCE: %f\n", in.distance);
//                    SmartDashboard.putNumber("TARGET DISTANCE (ft): ", in.distance);
//                    SmartDashboard.putNumber("TARGET DISTANCE (in): ", in.distance*12);

                }
                if (!(objects.size() == 0))
                {
                    out = objects.get(0);
                    out.time = System.currentTimeMillis();
                    Rect rec = new NIVision.Rect((int) out.bboxtop + 1,
                            (int) out.bboxleft - 1, (int) out.bboxheight + 2,
                            (int) out.bboxwidth + 2);
                    imaqDrawShapeOnImage(display, display, rec,
                            DrawMode.DRAW_VALUE, ShapeMode.SHAPE_RECT,
                            (float) 127.0);
                    imaqDrawLineOnImage(display, display, DrawMode.DRAW_VALUE,
                            new Point((int) out.xpos - 10, (int) out.ypos),
                            new Point((int) out.xpos - 10, (int) out.ypos + 10),
                            255f);
                    imaqDrawLineOnImage(display, display, DrawMode.DRAW_VALUE,
                            new Point((int) out.xpos, (int) out.ypos + 10),
                            new Point((int) out.xpos, (int) out.ypos + 10),
                            255f);
                } else
                {
                    out = null;
                }
                CameraServer.getInstance().setImage(display);
                RobotLogger.getInstance()
                        .log("========END OF SCORES========\n");
                Thread.sleep(rateLimit);
            } catch (Exception ex)
            {
                RobotLogger.getInstance().exc("VisionThread.run()", ex); 
                NIVision.IMAQdxCloseCamera(id);
                Robot.vision.runThread();
            } finally
            {
            }
        }
    }
    public void setRateLimit(long r8)
    {
    	rateLimit = r8;
    }

}