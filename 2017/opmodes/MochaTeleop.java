
package opmodes;

/*
 * FTC Team 5218: izzielau, October 30, 2016
 */

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.LightSensor;
import com.qualcomm.robotcore.hardware.Servo;

import team25core.DeadmanMotorTask;
import team25core.FourWheelDriveTask;
import team25core.GamepadTask;
import team25core.Robot;
import team25core.RobotEvent;

@TeleOp(name="5218 Mocha", group = "5218")
public class MochaTeleop extends Robot {

    private final static double SHOOTER_Y = MochaCalibration.SHOOTER_Y;
    private final static double SHOOTER_B = MochaCalibration.SHOOTER_B;
    private final static double BRUSH_SPEED = MochaCalibration.BRUSH_SPEED;

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor shooterLeft;
    private DcMotor shooterRight;
    private DcMotor sbod;
    private DcMotor capball;
    private Servo beacon;
    private Servo stopper;
    private Servo ranger;
    private DeviceInterfaceModule interfaceModule;
    private LightSensor one;
    private LightSensor two;

    protected boolean stopperIsStowed;

    @Override
    public void init()
    {
        // CDIM.
        interfaceModule = hardwareMap.deviceInterfaceModule.get("interface");

        // Light sensors.
        one = hardwareMap.lightSensor.get("lightLeft");
        one.enableLed(false);
        two = hardwareMap.lightSensor.get("lightRight");
        two.enableLed(false);

        // Drivetrain.
        frontRight = hardwareMap.dcMotor.get("motorFR");
        frontLeft = hardwareMap.dcMotor.get("motorFL");
        backRight = hardwareMap.dcMotor.get("motorBR");
        backLeft = hardwareMap.dcMotor.get("motorBL");
        frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Flywheels.
        shooterLeft = hardwareMap.dcMotor.get("shooterLeft");
        shooterRight = hardwareMap.dcMotor.get("shooterRight");
        shooterLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooterRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // SBOD.
        sbod = hardwareMap.dcMotor.get("brush");

        // Servo.
        beacon = hardwareMap.servo.get("beacon");
        beacon.setPosition(MochaCalibration.BEACON_STOWED_POSITION);
        stopper = hardwareMap.servo.get("stopper");
        stopper.setPosition(MochaCalibration.STOPPER_STOW_POSITION);
        ranger = hardwareMap.servo.get("ranger");
        ranger.setPosition(MochaCalibration.RANGE_PERPENDICULAR_POSITION);
        // Cap ball.
        capball = hardwareMap.dcMotor.get("capball");

        // Boolean values.
        stopperIsStowed = true;
    }

    @Override
    public void handleEvent(RobotEvent e) {

    }

    @Override
    public void start() {
        /* DRIVER ONE */
        // Four motor drive.
        final FourWheelDriveTask drive = new FourWheelDriveTask(this, frontLeft, frontRight, backLeft, backRight);
        this.addTask(drive);

        // SBOD
        DeadmanMotorTask collect = new DeadmanMotorTask(this, sbod, BRUSH_SPEED, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.RIGHT_BUMPER);
        addTask(collect);
        DeadmanMotorTask dispense = new DeadmanMotorTask(this, sbod, -BRUSH_SPEED, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.RIGHT_TRIGGER);
        addTask(dispense);

        // Shooters
        DeadmanMotorTask shootLeftY = new DeadmanMotorTask(this, shooterLeft, SHOOTER_Y, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.BUTTON_Y);
        addTask(shootLeftY);
        DeadmanMotorTask shootRightY = new DeadmanMotorTask(this, shooterRight, -SHOOTER_Y, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.BUTTON_Y);
        addTask(shootRightY);
        DeadmanMotorTask shootLeftB = new DeadmanMotorTask(this, shooterLeft, SHOOTER_B, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.BUTTON_B);
        addTask(shootLeftB);
        DeadmanMotorTask shootRightB = new DeadmanMotorTask(this, shooterRight, -SHOOTER_B, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.BUTTON_B);
        addTask(shootRightB);
        DeadmanMotorTask shootLeftBackwards = new DeadmanMotorTask(this, shooterLeft, -0.251, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.BUTTON_X);
        addTask(shootLeftBackwards);
        DeadmanMotorTask shootRightBackwards = new DeadmanMotorTask(this, shooterRight, 0.251, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.BUTTON_X);
        addTask(shootRightBackwards);

        /* DRIVER TWO */
        DeadmanMotorTask capBallUp = new DeadmanMotorTask(this, capball, 1.0, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.LEFT_BUMPER);
        addTask(capBallUp);
        DeadmanMotorTask capBallDown = new DeadmanMotorTask(this, capball, -0.5, GamepadTask.GamepadNumber.GAMEPAD_2, DeadmanMotorTask.DeadmanButton.LEFT_TRIGGER);
        addTask(capBallDown);

        this.addTask(new GamepadTask(this, GamepadTask.GamepadNumber.GAMEPAD_1) {
            public void handleEvent(RobotEvent e) {
                GamepadEvent event = (GamepadEvent) e;

                if (event.kind == EventKind.LEFT_TRIGGER_DOWN) {
                    beacon.setPosition(1.0);
                } else if (event.kind == EventKind.LEFT_BUMPER_DOWN) {
                    beacon.setPosition(MochaCalibration.BEACON_STOWED_POSITION);
                } else if (event.kind == EventKind.BUTTON_B_DOWN) {
                    drive.slowDown(true);
                    drive.slowDown(0.4);
                } else if (event.kind == EventKind.BUTTON_A_DOWN) {
                    drive.slowDown(true);
                    drive.slowDown(1.0);
                }
            }
        });

        this.addTask(new GamepadTask(this, GamepadTask.GamepadNumber.GAMEPAD_2) {
            public void handleEvent(RobotEvent e) {
                GamepadEvent event = (GamepadEvent) e;

                if (event.kind == EventKind.BUTTON_A_DOWN) {
                   if (stopperIsStowed) {
                       stopperIsStowed = false;
                       stopper.setPosition(MochaCalibration.STOPPER_STOP_POSITION);
                   } else if (!stopperIsStowed) {
                       stopperIsStowed = true;
                       stopper.setPosition(MochaCalibration.STOPPER_STOW_POSITION);
                   }
                }
            }
        });
    }
}
