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

package Inception.Skystone;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;


/**
 * This file houses Autonomous code
 * It uses the main robot class Refbot to define the hardware and driving commands
 * The code is structured as a LinearOpMode
 *
 * The code REQUIRES that you DO have encoders on the wheels
 *
 *  This code ALSO requires that the drive Motors have been configured such that a positive
 *  power command moves them forwards, and causes the encoders to count UP.
 *
 *
 *  See the Refbot class for encode-based driving controls that perform the actual movement.
 *
 */
@Autonomous(name="Incep: Auto Foundation Red", group="IncepBot")
public class IncepAuto_Foundation_Red extends LinearOpMode {

    /* Declare OpMode members. */
    private IncepBot         robot   = new IncepBot();   // Use a Pushbot's hardware

    private static final double     DRIVE_SPEED             = 0.9;
    private static final double     TURN_SPEED              = 0.6;
    private static final double     PIVOT_SPEED             = 0.40;
    private static final double     SQ                      = 70/3.0;        // Length of 3 squares / 3 in case we want to think that way
    private static final double     FDC                     = 2.0;

    @Override
    public void runOpMode() {

        /*
         * Initialize the drive system variables.
         * The init() method of the hardware class does all the work here
         */
        robot.init(hardwareMap);
        robot.initAutonomous(this);

        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        robot.releaseFoundation();

        robot.encoderStraight(DRIVE_SPEED,-34,4);

        robot.grabFoundation();

        robot.encoderStraight(DRIVE_SPEED,18,4);
        robot.encoderArc(PIVOT_SPEED, 90*FDC, IncepBot.RIGHT, 2, 5);

        robot.encoderStraight(DRIVE_SPEED,-8,4);
        robot.releaseFoundation();
        robot.encoderRotate(TURN_SPEED, -10, 2);
        robot.encoderStraight(DRIVE_SPEED,40,4);

        telemetry.addData("Path", "Complete");
        telemetry.update();
    }
}

/*
robot.releaseFoundation();

        robot.encoderStraight(DRIVE_SPEED,-32,3);

        robot.grabFoundation();

        robot.encoderStraight(DRIVE_SPEED,22,3);

        robot.releaseFoundation();

        robot.encoderRotate(TURN_SPEED, 90,3);
        robot.encoderStraight(DRIVE_SPEED,30,3);
        robot.encoderRotate(TURN_SPEED, 90,3);
        robot.encoderStraight(DRIVE_SPEED,42,4);
        robot.encoderRotate(TURN_SPEED, 90,3);
        robot.encoderStraight(DRIVE_SPEED,30,3);
        robot.encoderRotate(TURN_SPEED, -90,3);
        robot.encoderStraight(DRIVE_SPEED,-40,3);
        robot.encoderRotate(TURN_SPEED, -90,3);
        robot.encoderStraight(DRIVE_SPEED,-40,3);
 */