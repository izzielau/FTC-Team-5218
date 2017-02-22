package opmodes;

import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.LightSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.lang.annotation.Target;

import team25core.AlignWithWhiteLineTask;
import team25core.AutonomousEvent;
import team25core.ColorSensorTask;
import team25core.DeadReckon;
import team25core.DeadReckonTask;
import team25core.FourWheelDirectDriveDeadReckon;
import team25core.FourWheelDirectDrivetrain;
import team25core.GamepadTask;
import team25core.LightSensorCriteria;
import team25core.PeriodicTimerTask;
import team25core.PersistentTelemetryTask;
import team25core.Robot;
import team25core.RobotEvent;
import team25core.RunToEncoderValueTask;
import team25core.SingleShotTimerTask;

/**
 * Created by Lizzie on 11/19/2016.
 */
@Autonomous(name = "Particle Beacon", group = "5218")
public class MochaParticleBeaconAutonomous extends Robot {

    protected enum Alliance {
        BLUE,
        RED,
        DEFAULT,
    }

    protected enum StartingPosition {
        CORNER,
        VORTEX,
        DEFAULT,
    }

    public enum NumberOfBeacons {
        ONE,
        TWO,
        DEFAULT,
    }

    protected enum AimForCapBall {
        YES,
        NO,
        DEFAULT,
    }

    protected Alliance alliance;
    protected StartingPosition startingPosition;
    protected NumberOfBeacons numberOfBeacons;
    protected AimForCapBall aimForCapBall;

    private final int TICKS_PER_INCH = MochaCalibration.TICKS_PER_INCH;
    private final int TICKS_PER_DEGREE = MochaCalibration.TICKS_PER_DEGREE;
    private final double TURN_SPEED = MochaCalibration.TURN_SPEED;
    private final double MOVE_SPEED = MochaCalibration.MOVE_SPEED;
    private final double LINE_SPEED = MochaCalibration.LINE_SPEED;
    private final double LIGHT_MIN = MochaCalibration.LIGHT_MINIMUM;
    private final double LIGHT_MAX = MochaCalibration.LIGHT_MAXIMUM;
    private final double SHOOTER_CORNER = MochaCalibration.SHOOTER_AUTO_CORNER;
    private final double SHOOTER_VORTEX = MochaCalibration.SHOOTER_AUTO_VORTEX;
    private final double RANGE_DISTANCE_MINIMUM = MochaCalibration.RANGE_DISTANCE_MINIMUM;
    private final double BEACON_TICKS_PER_CM = MochaCalibration.BEACON_TICKS_PER_CM;

    private static int MOVE_MULTIPLIER = 0;
    private static int TURN_MULTIPLIER = 0;

    private ColorSensorTask.TargetColor targetColor;
    private int colorThreshold = 0;

    private int timeToPresserDeployed = 0;

    private boolean isOnSecondBeacon;
    private boolean isBlueAlliance;

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor sbod;
    private Servo beacon;
    private Servo stopper;
    private Servo ranger;
    private LightSensor rightLight;
    private LightSensor leftLight;
    private ModernRoboticsI2cRangeSensor rangeSensor;
    private DeviceInterfaceModule deviceInterfaceModule;
    private ColorSensor color;

    private RunToEncoderValueTask scoreCenterEncoderTask;
    private PersistentTelemetryTask persistentTelemetryTask;
    private GamepadTask gamepad;
    private double distanceFromWall;
    private double ticksToMove;

    private FourWheelDirectDriveDeadReckon positionForBeacon;
    private FourWheelDirectDriveDeadReckon targetingLine;
    private FourWheelDirectDriveDeadReckon alignColorSensorWithButton;
    private FourWheelDirectDriveDeadReckon moveToNextButton;
    private FourWheelDirectDriveDeadReckon compensationTurn;

    private FourWheelDirectDrivetrain drivetrain;

    private LightSensorCriteria rightSeesWhite;
    private LightSensorCriteria leftSeesWhite;
    private LightSensorCriteria rightSeesBlack;
    private LightSensorCriteria leftSeesBlack;

    private VelocityVortexBeaconArms beaconArms;

    @Override
    public void handleEvent(RobotEvent e) {
        if (e instanceof GamepadTask.GamepadEvent) {
            GamepadTask.GamepadEvent event = (GamepadTask.GamepadEvent) e;
            handleGamepadSelection(event);
        } else if (e instanceof AutonomousEvent) {
            AutonomousEvent event = (AutonomousEvent) e;
            handleBeaconWorkDone(event);
        }
    }

    public void handleGamepadSelection(GamepadTask.GamepadEvent event) {
        switch (event.kind) {
            case BUTTON_X_DOWN:
                alliance = Alliance.BLUE;
                persistentTelemetryTask.addData("ALLIANCE", "" + alliance);
                break;
            case BUTTON_B_DOWN:
                alliance = Alliance.RED;
                persistentTelemetryTask.addData("ALLIANCE", "" + alliance);
                break;
            case BUTTON_Y_DOWN:
                startingPosition = StartingPosition.CORNER;
                persistentTelemetryTask.addData("POSITION", "" + startingPosition);
                break;
            case BUTTON_A_DOWN:
                startingPosition = StartingPosition.VORTEX;
                persistentTelemetryTask.addData("POSITION", "" + startingPosition);
                break;
            case RIGHT_BUMPER_DOWN:
                numberOfBeacons = NumberOfBeacons.ONE;
                persistentTelemetryTask.addData("NUMBER OF BEACONS", "" + numberOfBeacons);
                break;
            case RIGHT_TRIGGER_DOWN:
                numberOfBeacons = NumberOfBeacons.TWO;
                persistentTelemetryTask.addData("NUMBER OF BEACONS", "" + numberOfBeacons);
                break;
            case LEFT_BUMPER_DOWN:
                aimForCapBall = AimForCapBall.YES;
                persistentTelemetryTask.addData("PUSH CAP BALL", "" + aimForCapBall);
                break;
            case LEFT_TRIGGER_DOWN:
                aimForCapBall = AimForCapBall.NO;
                persistentTelemetryTask.addData("PUSH CAP BALL", "" + aimForCapBall);
                break;
        }
    }

    @Override
    public void init()
    {
        // Assign globals to default states to prevent errors.
        alliance = Alliance.DEFAULT;
        startingPosition = StartingPosition.DEFAULT;
        numberOfBeacons = NumberOfBeacons.DEFAULT;
        aimForCapBall = AimForCapBall.DEFAULT;

        gamepad = new GamepadTask(this, GamepadTask.GamepadNumber.GAMEPAD_1);
        addTask(gamepad);

        persistentTelemetryTask = new PersistentTelemetryTask(this);
        addTask(persistentTelemetryTask);

        persistentTelemetryTask.addData("ALLIANCE", "NOT SELECTED");
        persistentTelemetryTask.addData("POSITION", "NOT SELECTED");
        persistentTelemetryTask.addData("NUMBER OF BEACONS", "NOT SELECTED");
        persistentTelemetryTask.addData("PUSH CAP BALL", "NOT SELECTED");

        frontLeft = hardwareMap.dcMotor.get("motorFL");
        frontRight = hardwareMap.dcMotor.get("motorFR");
        backLeft = hardwareMap.dcMotor.get("motorBL");
        backRight = hardwareMap.dcMotor.get("motorBR");


        beacon = hardwareMap.servo.get("beacon");
        beacon.setPosition(MochaCalibration.BEACON_STOWED_POSITION);
        stopper = hardwareMap.servo.get("stopper");
        stopper.setPosition(MochaCalibration.STOPPER_STOW_POSITION);
        ranger = hardwareMap.servo.get("ranger");
        ranger.setPosition(MochaCalibration.RANGE_PERPENDICULAR_POSITION);

        sbod = hardwareMap.dcMotor.get("brush");
        isOnSecondBeacon = false;
        isBlueAlliance = false;

        scoreCenterEncoderTask = new RunToEncoderValueTask(this, sbod, 50, 0.8);

        deviceInterfaceModule = hardwareMap.deviceInterfaceModule.get("interface");
        deviceInterfaceModule.setDigitalChannelMode(0, DigitalChannelController.Mode.OUTPUT);
        deviceInterfaceModule.setDigitalChannelState(0, false);

        rangeSensor = hardwareMap.get(ModernRoboticsI2cRangeSensor.class, "rangeSensor");
        color = hardwareMap.colorSensor.get("color");

    }

    protected void blueInit() {

        targetColor = ColorSensorTask.TargetColor.BLUE;
        colorThreshold = MochaCalibration.BLUE_COLOR_THRESHOLD;

        MOVE_MULTIPLIER = 1;
        TURN_MULTIPLIER = 1;

        rightLight = hardwareMap.lightSensor.get("lightLeft");
        leftLight = hardwareMap.lightSensor.get("lightRight");
        rightLight.enableLed(true);
        leftLight.enableLed(true);

        rightSeesWhite = new LightSensorCriteria(rightLight, LightSensorCriteria.LightPolarity.WHITE, LIGHT_MIN, LIGHT_MAX);
        rightSeesWhite.setThreshold(0.65);
        leftSeesWhite = new LightSensorCriteria(leftLight, LightSensorCriteria.LightPolarity.WHITE, LIGHT_MIN, LIGHT_MAX);
        leftSeesWhite.setThreshold(0.65);
        rightSeesBlack = new LightSensorCriteria(rightLight, LightSensorCriteria.LightPolarity.BLACK, LIGHT_MIN, LIGHT_MAX);
        rightSeesBlack.setThreshold(0.65);
        leftSeesBlack = new LightSensorCriteria(leftLight, LightSensorCriteria.LightPolarity.BLACK, LIGHT_MIN, LIGHT_MAX);
        leftSeesBlack.setThreshold(0.65);

        drivetrain = new FourWheelDirectDrivetrain(MochaCalibration.TICKS_PER_INCH, MochaCalibration.BLUE_PIVOT_MULTIPLIER,
                backLeft, frontLeft, backRight, frontRight);

        drivetrain.resetEncoders();
        drivetrain.encodersOn();

    }

    protected void redInit() {

        targetColor = ColorSensorTask.TargetColor.RED;
        colorThreshold = MochaCalibration.RED_COLOR_THRESHOLD;

        MOVE_MULTIPLIER = -1;
        TURN_MULTIPLIER = -1;

        rightLight = hardwareMap.lightSensor.get("lightRight");
        leftLight = hardwareMap.lightSensor.get("lightLeft");
        rightLight.enableLed(true);
        leftLight.enableLed(true);

        rightSeesWhite = new LightSensorCriteria(rightLight, LightSensorCriteria.LightPolarity.WHITE, LIGHT_MIN, LIGHT_MAX);
        rightSeesWhite.setThreshold(0.65);
        leftSeesWhite = new LightSensorCriteria(leftLight, LightSensorCriteria.LightPolarity.WHITE, LIGHT_MIN, LIGHT_MAX);
        leftSeesWhite.setThreshold(0.65);
        rightSeesBlack = new LightSensorCriteria(rightLight, LightSensorCriteria.LightPolarity.BLACK, LIGHT_MIN, LIGHT_MAX);
        rightSeesBlack.setThreshold(0.65);
        leftSeesBlack = new LightSensorCriteria(leftLight, LightSensorCriteria.LightPolarity.BLACK, LIGHT_MIN, LIGHT_MAX);
        leftSeesBlack.setThreshold(0.65);

        drivetrain = new FourWheelDirectDrivetrain(MochaCalibration.TICKS_PER_INCH, MochaCalibration.RED_PIVOT_MULTIPLIER,
                frontRight, backRight, frontLeft, backLeft);

        drivetrain.resetEncoders();
        drivetrain.encodersOn();

    }

    @Override
    public void start()
    {
        if (alliance == Alliance.RED) {
            redInit();
            isBlueAlliance = false;
        } else {
            blueInit();
            isBlueAlliance = true;
        }

        RobotLog.i("163 ========================= START ========================= ");
        alignToBeacon(45);
        // handleReadyToMoveServo();
    }

    protected void alignToBeacon(int inchesToDiscard)
    {
        drivetrain.setCanonicalMotorDirection();
        addTask(new AlignWithWhiteLineTask(this, inchesToDiscard, drivetrain, leftSeesBlack, leftSeesWhite, rightSeesBlack, rightSeesWhite) {
            @Override
            public void handleEvent(RobotEvent e) {
                AlignWithWhiteLineEvent ev = (AlignWithWhiteLineEvent)e;
                if (ev.kind == EventKind.FINE_TUNING) {
                   handleReadyToMoveServo();
                } if (ev.kind == EventKind.ALIGNED || ev.kind == EventKind.GOOD_ENOUGH) {
                    handleAlignedWithWhiteLine();
                } else {
                    RobotLog.e("Didn't find the white line, aborting");
                }
            }
        });

    }

    ElapsedTime elapsedTime;
    protected void handleReadyToMoveServo()
    {
        addTask(new PeriodicTimerTask(this, 40){
            @Override
            public void handleEvent(RobotEvent e) {
                distanceFromWall = rangeSensor.getDistance(DistanceUnit.CM);
                RobotLog.i("163 Range sensor distance %f", distanceFromWall);
                if (distanceFromWall != 255) {
                    this.stop();

                    double readPosition = ((distanceFromWall - 12) * MochaCalibration.BEACON_TICKS_PER_CM/(float)256.0) + MochaCalibration.BEACON_STOWED_POSITION;
                    beacon.setPosition(readPosition);

                    timeToPresserDeployed = 250 * (int)(distanceFromWall - 12);
                    RobotLog.i("163 Time to presser deployed: " + timeToPresserDeployed);

                    elapsedTime = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
                    elapsedTime.reset();
                    RobotLog.i("163 Started moving servo");
                } else {
                    handleReadyToMoveServo();
                }
            }
        });
    }

    protected void handleAlignedWithWhiteLine()
    {
        RobotLog.i("163 Moving backwards to align the color sensor with the beacon");
        persistentTelemetryTask.addData("AUTONOMOUS STATE: ", "Aligning light sensor");

        frontRight.setDirection(DcMotorSimple.Direction.REVERSE);
        backRight.setDirection(DcMotorSimple.Direction.REVERSE);
        frontLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        backLeft.setDirection(DcMotorSimple.Direction.FORWARD);

        alignColorSensorWithButton = new FourWheelDirectDriveDeadReckon
                (this, TICKS_PER_INCH, TICKS_PER_DEGREE, frontRight, backRight, frontLeft, backLeft);
        if (isBlueAlliance) {
            alignColorSensorWithButton.addSegment(DeadReckon.SegmentType.STRAIGHT, 2.5, 0.25 * -MOVE_SPEED);
        } else {
            alignColorSensorWithButton.addSegment(DeadReckon.SegmentType.STRAIGHT, 2, 0.25 * -MOVE_SPEED);
        }

        addTask(new DeadReckonTask(this, alignColorSensorWithButton) {
            @Override
            public void handleEvent(RobotEvent e) {
                DeadReckonEvent event = (DeadReckonEvent) e;

                if (event.kind == EventKind.PATH_DONE) {
                    RobotLog.i("163 Color sensor has aligned with the beacon");
                    persistentTelemetryTask.addData("AUTONOMOUS STATE: ", "Aligned color sensor");

                    addTask(new SingleShotTimerTask(robot, 750) {
                        @Override
                        public void handleEvent(RobotEvent e)
                        {
                            SingleShotTimerEvent event = (SingleShotTimerEvent) e;
                            if (event.kind == EventKind.EXPIRED) {
                                determineColor();
                            }
                        }
                    });
                } else {
                    RobotLog.e("163 Unknown event occurred");
                }
             }
        });
    }

    protected void determineColor()
    {
        RobotLog.i("Determining beacon color");
        if (elapsedTime.time() < timeToPresserDeployed) {
            addTask(new SingleShotTimerTask(this, timeToPresserDeployed - (int) elapsedTime.time()) {
                @Override
                public void handleEvent(RobotEvent e) {
                    senseColor();
                }
            });
        } else {
            senseColor();
        }
    }

    protected void senseColor()
    {
        double compensation;
        if (isBlueAlliance) {
            compensation = 3.25;
        } else {
            compensation = 2.5;
        }

        compensationTurn = new FourWheelDirectDriveDeadReckon
                (this, MochaCalibration.TICKS_PER_INCH, MochaCalibration.TICKS_PER_DEGREE, frontRight, backRight, frontLeft, backLeft);
        compensationTurn.addSegment(DeadReckon.SegmentType.TURN, 1, MOVE_MULTIPLIER * -MochaCalibration.TURN_SPEED);

        moveToNextButton = new FourWheelDirectDriveDeadReckon
                (this, MochaCalibration.TICKS_PER_INCH, MochaCalibration.TICKS_PER_DEGREE, frontRight, backRight, frontLeft, backLeft);
        moveToNextButton.addSegment(DeadReckon.SegmentType.STRAIGHT, compensation, 0.5 * -MochaCalibration.MOVE_SPEED);

        beaconArms = new VelocityVortexBeaconArms(this, deviceInterfaceModule, color, moveToNextButton, compensationTurn, beacon, isBlueAlliance, numberOfBeacons, distanceFromWall);
        ColorSensorTask colorTask = new ColorSensorTask(this, color, deviceInterfaceModule, false, 0) {
            @Override
            public void handleEvent(RobotEvent e) {
                ColorSensorEvent event = (ColorSensorEvent) e;
                switch (event.kind) {
                    case YES:
                        RobotLog.i("163 Color is correct");
                        beaconArms.deploy(true, !isOnSecondBeacon);
                        break;
                    case NO:
                        RobotLog.i("163 Color is on the next button");
                        beaconArms.deploy(false, !isOnSecondBeacon);
                        break;
                }
            }
        };

        colorTask.setModeSingle(targetColor, colorThreshold);
        colorTask.setMsDelay(MochaCalibration.COLOR_READ_DELAY);
        colorTask.setReflectColor(true, hardwareMap);
        addTask(colorTask);
    }

    protected void handleBeaconWorkDone(AutonomousEvent e)
    {
        if (!isOnSecondBeacon) {
            RobotLog.i("163 Beacon work for beacon one is done");
            if (e.kind == AutonomousEvent.EventKind.BEACON_DONE) {
                RobotLog.i("163 Autonomous event is of type BeaconDone");
                isOnSecondBeacon = true;
                alignToBeacon(33);
            }
        } else {
            RobotLog.i("163 Beacon work called after first beacon");
            if (e.kind == AutonomousEvent.EventKind.BEACON_DONE) {
                if (aimForCapBall == AimForCapBall.YES) {
                    handleReadyForCapBall();
                }
            }
        }
    }

    public void handleReadyForCapBall()
    {

        frontRight.setDirection(DcMotorSimple.Direction.REVERSE);
        backRight.setDirection(DcMotorSimple.Direction.REVERSE);
        frontLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        backLeft.setDirection(DcMotorSimple.Direction.FORWARD);

        FourWheelDirectDriveDeadReckon targetCapBall = new FourWheelDirectDriveDeadReckon
                (this, TICKS_PER_INCH, TICKS_PER_DEGREE, frontRight, backRight, frontLeft, backLeft);
        targetCapBall.addSegment(DeadReckon.SegmentType.TURN, 45, 0.5 * -MOVE_SPEED * MOVE_MULTIPLIER);
        targetCapBall.addSegment(DeadReckon.SegmentType.STRAIGHT, 45, MOVE_SPEED * MOVE_MULTIPLIER);

        RobotLog.i("163 Targeting the cap ball");
        addTask(new DeadReckonTask(this, targetCapBall) {
            @Override
            public void handleEvent(RobotEvent e)
            {
                DeadReckonEvent event = (DeadReckonEvent) e;
                if (event.kind == EventKind.PATH_DONE) {
                    RobotLog.i("163 Finished moving to the center vortex platform");
                }
            }
        });
    }
}
