package opmodes;

/**
 * Created by Elizabeth on 12/8/2016.
 */
public class MochaCalibration {

    public static final int TICKS_PER_DEGREE = 17;
    public static final int TICKS_PER_INCH = 79;
    public static final double BLUE_PIVOT_MULTIPLIER = 2.7;
    public static final double RED_PIVOT_MULTIPLIER = 2.0;

    public static final double SHOOTER_AUTO_VORTEX = 0.5;
    public static final double SHOOTER_AUTO_CORNER = 0.425;

    public static final double SHOOTER_Y = 0.65;
    public static final double SHOOTER_B = 0.715;

    public static final double LIGHT_MINIMUM = 1.35;
    public static final double LIGHT_MAXIMUM = 2.01;

    public static final double TURN_SPEED = 0.2;
    public static final double MOVE_SPEED = 0.75;
    public static final double LINE_SPEED = 0.2;
    public static final double BRUSH_SPEED = 0.8;

    public static final double RANGE_DISTANCE_MINIMUM = 12;

    public static final double BEACON_TICKS_PER_CM = 5.7742;
    public static final double BEACON_STOWED_POSITION = 125/(float)256.0;
    public static final double STOPPER_STOW_POSITION = 116/(float)256.0;
    public static final double STOPPER_STOP_POSITION = 0/(float)256.0;
    public static final double RANGE_PERPENDICULAR_POSITION = 147/(float)256.0;
    public static final double RANGE_ANGLED_POSITION = 219/(float)256.0;

    public static final int BLUE_COLOR_THRESHOLD = 1900;
    public static final int RED_COLOR_THRESHOLD = 1500;
    public static final int COLOR_READ_DELAY = 700;
}
