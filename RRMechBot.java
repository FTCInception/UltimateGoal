/* Copyright (c) 2017 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package Inception.UltimateGoal;

import com.acmerobotics.roadrunner.util.NanoClock;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;


import java.util.Locale;


/**
 * This is NOT an opmode.
 *
 * This class can be used to define all the specific hardware for a single robot.
 *
 * This hardware class assumes the following device names have been configured on the robot:
 * Note:  All names are lower case and some have single spaces between words.
 *
 * Motor channel:  Left  drive motor:        "left_front"
 * Motor channel:  Right drive motor:        "right_front"
 * Motor channel:  Left  drive motor:        "left_back"
 * Motor channel:  Right drive motor:        "right_back"
 * Servo channel:  Servo to grab foundation: "foundation"
 * Servo channel:  Servo to grab blocks:     "claw"
 */
public class RRMechBot {
    /* Public OpMode members. */
    public DcMotor intake_motor;
    public DcMotorEx shoot1_motor, shoot2_motor;
    public DcMotorEx wobble_motor;
    public Servo claw=null,flicker=null;
    public SampleMecanumDrive drive = null;

    public int WOBBLE_START = 0;
    public int WOBBLE_PICKUP = 1;
    public int WOBBLE_CARRY = 2;
    public int WOBBLE_DROP = 3;

    //final double WOBBLE_TICKS_PER_DEGREE = 5264.0/360.0; // 30 RPM 6mm d-shaft (5202 series)
    //final double WOBBLE_TICKS_PER_DEGREE = 2786.0/360.0; // 60 RPM 6mm d-shaft (5202 series)
    final double startingAngle = 15.0;
    final double WOBBLE_TICKS_PER_DEGREE = 3892.0/360.0; // 43 RPM 8mm REX (5203 series)
    final int wobbleTargets[] = {(int)(5*WOBBLE_TICKS_PER_DEGREE),(int)((235-startingAngle)*WOBBLE_TICKS_PER_DEGREE), (int)((180-startingAngle)*WOBBLE_TICKS_PER_DEGREE),(int)((225-startingAngle)*WOBBLE_TICKS_PER_DEGREE)};

    public BotLog logger = new BotLog();

    /* local OpMode members. */
    HardwareMap hardwareMap = null;
    private static LinearOpMode myLOpMode;


    // Shooter PID related
    private double P = 0.0005;
    private double I = 0.0000003;
    private double D = 0.00007;
    private double F = 1.0/7250.0;  // @ 13V resting (12.8V under load)
    private double vF = F;          // Voltage adjusted F
    // SW PID with some limits
    MiniPID pid = new MiniPID(P,I,D,F);
    private NanoClock PIDClock = NanoClock.system();
    private double shooterRPM = 0.0 ;
    private double PIDTime = 0.05;
    private double PIDStartTime = PIDClock.seconds();
    private double nextPID = PIDStartTime;
    private int PIDAvgSize = 10;
    private int PIDAvgCount = 0;
    private double[] Pos1 = new double[PIDAvgSize];
    private double[] Pos2 = new double[PIDAvgSize];
    private double[] Time1 = new double[PIDAvgSize];
    private double[] Time2 = new double[PIDAvgSize];
    private double lastPIDTime = PIDTime;
    private double prevPIDTime1 = PIDTime;
    private double prevPIDTime2 = PIDTime;
    private double myRPM1 = 0;
    private double myRPM2 = 0;
    private double myiRPM1 = 0;
    private double myiRPM2 = 0;
    private double output1 = 0;
    private double output2 = 0;
    private int singleMode = (int) (1/PIDTime);
    private VoltageSensor Vsense;
    private double shooterGearRatio = 1.5;
    private double prevShoot1Pos, prevShoot2Pos;


    /* Constructor */
    public RRMechBot() {
    }

    public double getBatteryVoltage() {
        double result = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            double voltage = sensor.getVoltage();
            if (voltage > 0) {
                result = Math.min(result, voltage);
            }
        }
        return result;
    }

    public void initAutonomous(LinearOpMode lOpMode) {
        myLOpMode = lOpMode;

        // Run the wobble arm for a teeny little bit at low power just to take out and slack...
        wobble_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        wobble_motor.setPower(-0.033);
        myLOpMode.sleep(500);
        wobble_motor.setPower(0.0);
        wobble_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        wobble_motor.setTargetPosition(0);
        wobble_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        
    }

    /* Initialize standard Hardware interfaces */
    public void init(HardwareMap ahwMap) {
        // Save reference to Hardware map
        hardwareMap = ahwMap;

        drive = new SampleMecanumDrive(hardwareMap);

        // Initialize the hardware variables. Note that the strings used here as parameters
        // to 'get' must correspond to the names assigned during the robot configuration
        // step (using the FTC Robot Controller app on the phone).

        intake_motor = hardwareMap.dcMotor.get("intake");
        intake_motor.setPower(0.0);
        intake_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        intake_motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        intake_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        intake_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        //intake_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        // Zero out the wobble motor in the auto init
        wobble_motor = hardwareMap.get(DcMotorEx.class,"wobble");
        wobble_motor.setPower(0.0);
        //wobble_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        wobble_motor.setDirection(DcMotorSimple.Direction.FORWARD);
        wobble_motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        wobble_motor.setTargetPosition(0);
        wobble_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        wobble_motor.setPositionPIDFCoefficients(5.0);
        wobble_motor.setVelocityPIDFCoefficients(2.0,0.5,0.0,11.1);

        shoot1_motor = hardwareMap.get(DcMotorEx.class,"shoot1");
        shoot2_motor = hardwareMap.get(DcMotorEx.class,"shoot2");
        shoot1_motor.setPower(0.0);
        shoot2_motor.setPower(0.0);
        shoot1_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        shoot2_motor.setDirection(DcMotorSimple.Direction.REVERSE);
        shoot1_motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shoot2_motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shoot1_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shoot2_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // SW PID
        ////shoot1_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        ////shoot2_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        pid.setOutputLimits(0.0,0.90);
        pid.setMaxIOutput(0.05);

        // REV PID
        // These PIDF values seem pretty good.
        //PIDFCoefficients pidFNew = new PIDFCoefficients(125.0, 2.5, 5.0, 4.0);
        // Raising 'F' increases overshoot a lot
        // Raising 'I' increases overshoot a lot
        // 'P' is in a sweet-spot, could go down to 75 and still be OK
        // 'D' didn't make a ton of different, not sure that is tuned properly
        // Quick spin-up and recovery.  There may be a little overshoot just after a shot.
        //shoot1_motor.setVelocityPIDFCoefficients(150.0, 0, 7.0, 12.0);
        //shoot2_motor.setVelocityPIDFCoefficients(150.0, 0, 7.0, 12.0);
        ////shoot1_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        ////shoot2_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        ////shoot1_motor.setVelocityPIDFCoefficients(45.0, 0, 30.0, 12.0);
        ////shoot2_motor.setVelocityPIDFCoefficients(45.0, 0, 30.0, 12.0);

        claw = hardwareMap.servo.get("claw");
        claw.setPosition(1.0);

        flicker = hardwareMap.servo.get("flicker");
        flicker.setPosition(0.0);

        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            if (Vsense == null) {
                double voltage = sensor.getVoltage();
                if (voltage > 0) {
                    Vsense = sensor;
                }
            }
        }

        // Voltage adjust F
        vF = (F*(12.8/Vsense.getVoltage()));
        pid.setF(vF);

    }

    public void setWobblePosition(int wobblePos, double power){
        wobble_motor.setTargetPosition(wobbleTargets[wobblePos]);
        wobble_motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        wobble_motor.setPower(power);
    }

    public void intakeStop(){
        intake_motor.setPower(0.0);
        intake_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public void intakeEjectWobble(double power){
        intake_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        intake_motor.setPower(power);
    }

    public void intakePickupRing(double power){
        intake_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intake_motor.setPower(power);
    }

    public double gyroRotatePrecise(double speed, double degrees, double timeoutS) {

        // This profile is for rotate
        double[] speedRampUpR = {0.225, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0};
        double[] speedRampDownR = {0.20, 0.225, 0.25, 0.275, 0.30, 0.325, 0.35, 0.375, 0.40, 0.45, 0.50, 0.55, 0.60, 0.65, 0.70, 0.75, 0.80, 0.85, 0.90, 0.95, 1.0};

        return(gyroRotate(degrees, speed, timeoutS, speedRampUpR, speedRampDownR, 0.25));
    }

    /*
     *  Method to perfmorm an arbitrary turn with differential wheel power and gyro feedback
     *  Move will stop if any of three conditions occur:
     *  1) Move gets to the desired heading
     *  2) Move runs out of time
     *  3) Driver stops the opmode running.
     */
    public double gyroRotate(double degrees,
                           double maxPower,
                           double timeoutS, double[] up, double[] down,
                           double precision ) {
        double toGo;
        double actSpeed=0.0;
        double newSpeed ;
        double spdUp, spdDn;
        double[] speedRampUp = up;
        double[] speedRampDown = down;
        ElapsedTime runtime = new ElapsedTime();
        double tgtHeading, curHeading, startHeading;
        double KsL = 1.0;
        double KsR = 1.0;
        double delta, sign=1.0;
        double rt = 0.0;
        double KpL = 1.0;
        double KpR = 1.0;

        // Ensure that the opmode is still active
        if (myLOpMode.opModeIsActive() && !myLOpMode.isStopRequested()) {

            // Get the current Heading
            startHeading = curHeading = Math.toDegrees(drive.getRawExternalHeading());
            // Where we are headed
            tgtHeading = AngleUnit.normalizeDegrees(curHeading + degrees) ;
            // Initialize toGo
            toGo = Math.abs(degrees);

            logger.logD("RRMechLog",String.format("gyroTurn: start: %f, tgt: %f", startHeading, tgtHeading));

            if (false) {
                myLOpMode.telemetry.addData("gyro", "d: %3.1f, c: %3.1f, s: %3.1f, t: %3.1f, e: %3.1f",degrees,curHeading,startHeading,tgtHeading,AngleUnit.normalizeDegrees(curHeading - tgtHeading));
                myLOpMode.telemetry.update();
                //myLOpMode.sleep(3000);
            }

            // Determine rotation direction
            if (degrees > 0){
                KsL = -1.0;
                KsR = 1.0;
            } else {
                KsL = 1.0;
                KsR = -1.0;
            }

            if (false) {
                myLOpMode.telemetry.addData("gyro", "KL: %.3f, KR: %.3f, max: %.2f",KsL, KsR,maxPower);
                myLOpMode.telemetry.update();
            }

            // reset the timeout time and start motion at the minimum speed.
            runtime.reset();

            // Setting the starting speed is now handled inside the loop below on the first iteration.
            delta = AngleUnit.normalizeDegrees(curHeading - tgtHeading);
            if ( delta != 0) {
                sign = delta/Math.abs(delta);
            }

            // Initialize variables before the loop
            // Use done to jump out of loop when you get close enough to the end
            // Keep looping while we are still active, and there is time left, and we haven't reached the target
            while (myLOpMode.opModeIsActive() &&
                    !myLOpMode.isStopRequested() &&
                    ((rt=runtime.seconds()) < timeoutS)) {

                // Check if we're done
                // This code implements a soft start and soft stop.
                curHeading = Math.toDegrees(drive.getRawExternalHeading());

                // Look to see if our delta is really close or if we over rotated already (sign changed on delta)
                delta = AngleUnit.normalizeDegrees(curHeading - tgtHeading);
                if (((Math.abs(delta)) < precision) || (delta/Math.abs(delta) != sign)) {
                    break;
                }

                // How much farther?
                toGo = Math.min(toGo, Math.max(0, Math.abs(AngleUnit.normalizeDegrees(tgtHeading - curHeading))));

                // Compute speed on acceleration and deceleration legs
                spdUp = Math.min(speedRampUp[Math.min((int) (Math.abs(AngleUnit.normalizeDegrees(startHeading - curHeading)) / 5.0), speedRampUp.length - 1)], maxPower);
                spdDn = Math.min(speedRampDown[Math.min((int) (Math.abs(toGo) / 5.0), speedRampDown.length - 1)], maxPower);

                // Scale the final speed against the input power.
                newSpeed = Math.min(spdDn, spdUp);

                // Only update the motors if we really need too
                if (newSpeed != actSpeed ) {

                    // Record the new base speed
                    actSpeed = newSpeed;

                    drive.setMotorPowers(Range.clip(newSpeed * KpL * KsL,-1.0,1.0),
                            Range.clip(newSpeed * KpL * KsL,-1.0,1.0),
                            Range.clip(newSpeed * KpR * KsR,-1.0,1.0),
                            Range.clip(newSpeed * KpR * KsR,-1.0,1.0));
                }

                if (false) {
                    myLOpMode.telemetry.addData("left", "s: %1.3f",newSpeed * KpL * KsL);
                    myLOpMode.telemetry.addData("right", "s: %1.3f",newSpeed * KpR * KsR);
                    myLOpMode.telemetry.addData("gyro", "deg: %1.3f, curr: %1.3f, start: %1.3f, tgt: %1.3f",degrees,curHeading,startHeading,tgtHeading);
                    myLOpMode.telemetry.update();
                }
                // FIXME -- Read the front encoders only for logging
            }

            // Stop all motion;
            drive.setMotorPowers(0,0,0,0);

            if (false) {
                myLOpMode.telemetry.addData("gyro", "d: %3.1f, c: %3.1f, s: %3.1f, t: %3.1f, e: %3.1f", degrees, curHeading, startHeading, tgtHeading, AngleUnit.normalizeDegrees(curHeading - tgtHeading));
                myLOpMode.telemetry.update();
                myLOpMode.sleep(1000);
            }

            double tmpHeading = Math.toDegrees(drive.getRawExternalHeading());
            logger.logD("RRMechLog",String.format("turn done: final: %f, get: %f, tgt: %f, err: %f", curHeading, tmpHeading, tgtHeading, tmpHeading - tgtHeading ));
            return(AngleUnit.normalizeDegrees(tmpHeading - tgtHeading));
        }
        return(0.0);
    }

    public void setShooter(double RPM, double power, boolean SWPID) {

        if (SWPID) {
            // Run a second without averaging if the delta is large
            if (Math.abs(RPM - shooterRPM) > 1000) {
                singleMode = (int) (1 / PIDTime);
            } else {
                singleMode = (int) (.5 / PIDTime);
            }

            // Set new RPM and reset PID variables
            shooterRPM = RPM;
            PIDAvgCount = 0;
            PIDStartTime = PIDClock.seconds();
            nextPID = PIDStartTime;
            pid.reset();

            shoot1_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            shoot2_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            shoot1_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            shoot2_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

            // Force an update, the first update only sets Feed-forward
            updateShooterPID();
        } else {
            shoot1_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            shoot2_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            shoot1_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            shoot2_motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            shoot1_motor.setVelocityPIDFCoefficients(45.0, 0, 30.0, 12.0);
            shoot2_motor.setVelocityPIDFCoefficients(45.0, 0, 30.0, 12.0);

            shoot1_motor.setPower(power);
            shoot1_motor.setPower(power);
        }
    }

    public void updateShooterPID() {
        double now = PIDClock.seconds() ;
        double shoot1Pos, shoot2Pos;
        double tps1, tps2;

        // If enough time has passed
        if ( nextPID < now ) {

            // And we are supposed to be spinning
            if ( shooterRPM > 0.0) {

                // Get each motor position, timestamp, and record it in our circular buffer
                shoot1Pos = shoot1_motor.getCurrentPosition();
                double t1 = PIDClock.seconds();

                shoot2Pos = shoot2_motor.getCurrentPosition();
                double t2 = PIDClock.seconds();

                int avgIndex = PIDAvgCount % PIDAvgSize;
                Pos1[avgIndex] = shoot1Pos;
                Time1[avgIndex] = t1;
                Pos2[avgIndex] = shoot2Pos;
                Time2[avgIndex] = t2;
                PIDAvgCount++;

                // Need 2 samples to do anything really useful
                if (PIDAvgCount > 1) {

                    // Check instant RPM for this sample, less lag but more noise
                    myiRPM1 = (((((shoot1Pos - prevShoot1Pos) / (t1 - prevPIDTime1)) / 28.0) * shooterGearRatio) * 60.0);
                    myiRPM2 = (((((shoot2Pos - prevShoot2Pos) / (t2 - prevPIDTime2)) / 28.0) * shooterGearRatio) * 60.0);

                    // Keep running without averaging until we get close to the target
                    if ((myiRPM1 < (shooterRPM*0.90)) || (myiRPM2 < (shooterRPM*0.90))) {
                        singleMode = (int) (.5 / PIDTime);
                    }

                    // Now go compute RPM
                    double deltaPos1;
                    double deltaPos2;
                    double deltaTime1;
                    double deltaTime2;

                    int avgIndexH = ((PIDAvgCount-1) % PIDAvgSize);
                    int avgIndexL = (PIDAvgCount % PIDAvgSize);
                    if (singleMode > 0 ) {
                        avgIndexL = ((PIDAvgCount-2) % PIDAvgSize);
                        singleMode--;
                    }

                    deltaPos1 = Pos1[avgIndexH] - Pos1[avgIndexL];
                    deltaPos2 = Pos2[avgIndexH] - Pos2[avgIndexL];
                    deltaTime1 = Time1[avgIndexH] - Time1[avgIndexL];
                    deltaTime2 = Time2[avgIndexH] - Time2[avgIndexL];

                    tps1 = deltaPos1 / deltaTime1;
                    tps2 = deltaPos2 / deltaTime2;

                    myRPM1 = (((tps1 / 28.0) * shooterGearRatio) * 60.0);
                    myRPM2 = (((tps2 / 28.0) * shooterGearRatio) * 60.0);

                    // Compute PID values and include some time skew if there was any
                    output1 = pid.getOutput(myRPM1, shooterRPM, ((t1-prevPIDTime1)/PIDTime));
                    output2 = pid.getOutput(myRPM2, shooterRPM, ((t2-prevPIDTime2)/PIDTime));

                    // Set the power
                    shoot1_motor.setPower(Range.clip(output1, 0.0, 0.9));
                    shoot2_motor.setPower(Range.clip(output2, 0.0, 0.9));

                    if (false) {
                        logger.logD("ShooterCSV", String.format(",%f,%f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%.2f,%.3f,%.3f,%.3f,%f,%f,%f,%.0f", now, now - lastPIDTime, shoot1Pos, shoot2Pos, shoot1Pos - prevShoot1Pos, shoot2Pos - prevShoot2Pos, myRPM1, myRPM2, myiRPM1, myiRPM2, output1, output2));
                    }
                } else {
                    // just set a fake feed-forward value on the first sample.
                    shoot1_motor.setPower(Range.clip((vF * shooterRPM), 0.0, 0.9));
                    shoot2_motor.setPower(Range.clip((vF * shooterRPM), 0.0, 0.9));
                }

                prevShoot1Pos = shoot1Pos;
                prevShoot2Pos = shoot2Pos;

                prevPIDTime1 = t1;
                prevPIDTime2 = t2;

                // 'Schedule' the next PID check
                nextPID = now + PIDTime;
                lastPIDTime = now;
            } else {
                shoot1_motor.setPower(0);
                shoot2_motor.setPower(0);
                // 'Schedule' the next PID check
                nextPID = now + PIDTime;
                lastPIDTime = now;
            }
        }
    }
}

