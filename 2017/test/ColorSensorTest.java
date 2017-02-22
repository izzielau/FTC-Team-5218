package test;

import com.qualcomm.hardware.ams.AMSColorSensor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DeviceInterfaceModule;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.I2cDeviceImpl;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.ServiceConfigurationError;

import team25core.AMSColorSensorImproved;
import team25core.ColorSensorTask;
import team25core.Robot;
import team25core.RobotEvent;

/**
 * Created by Izzie on 2/10/2016.
 */
@Autonomous(name = "TEST Color", group = "TEST")
public class ColorSensorTest extends Robot {

    private DeviceInterfaceModule interfaceModule;
    private AMSColorSensorImproved colorRight;

    @Override
    public void handleEvent(RobotEvent e) {

    }

    @Override
    public void init() {
        interfaceModule = hardwareMap.deviceInterfaceModule.get("interface");
        colorRight = AMSColorSensorImproved.create(AMSColorSensor.Parameters.createForAdaFruit(), new I2cDeviceImpl(interfaceModule, 0));

        interfaceModule.setDigitalChannelMode(0, DigitalChannelController.Mode.OUTPUT);
        interfaceModule.setDigitalChannelState(0, false);
    }

    @Override
    public void loop() {

        int rightRed = colorRight.red();
        int rightBlue = colorRight.blue();

        if (rightRed > rightBlue) {
            telemetry.addData("STATE (R): ", "Red");
        } else if (rightRed < rightBlue) {
            telemetry.addData("STATE (R): ", "Blue");
        } else {
            telemetry.addData("STATE (R): ", "Unknown");
        }

        telemetry.addData("RIGHT (R): ", rightRed);
        telemetry.addData("RIGHT (B): ", rightBlue);

    }
}
