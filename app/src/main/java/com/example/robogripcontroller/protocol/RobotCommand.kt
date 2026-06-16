package com.example.robogripcontroller.protocol

import kotlin.math.roundToInt

object RobotMapping {
    // Xe đang bị ngược tới/lùi nên để true
    const val INVERT_DRIVE_FORWARD = true

    // Nếu nghiêng/joystick trái phải bị ngược thì đổi thành true
    const val INVERT_DRIVE_TURN = false

    // Nếu nút NÂNG/HẠ lại điều khiển MỞ/ĐÓNG và ngược lại thì để true
    const val SWAP_ARM_AXES = true

    // Sau khi swap trục, nếu NÂNG thành HẠ thì đổi biến này
    const val INVERT_ARM_AXIS_1 = false

    // Sau khi swap trục, nếu MỞ thành ĐÓNG thì đổi biến này
    const val INVERT_ARM_AXIS_2 = false
}

object RobotCommand {

    fun drive(
        forward: Int,
        turn: Int,
        speed: Int
    ): String {
        val mappedForward = if (RobotMapping.INVERT_DRIVE_FORWARD) {
            -forward
        } else {
            forward
        }

        val mappedTurn = if (RobotMapping.INVERT_DRIVE_TURN) {
            -turn
        } else {
            turn
        }

        val safeForward = mappedForward.coerceIn(-127, 127)
        val safeTurn = mappedTurn.coerceIn(-127, 127)
        val safeSpeed = speed.coerceIn(80, 255)

        return "D,$safeForward,$safeTurn,$safeSpeed\n"
    }

    fun arm(
        axis1: Int,
        axis2: Int
    ): String {
        val swappedAxis1 = if (RobotMapping.SWAP_ARM_AXES) axis2 else axis1
        val swappedAxis2 = if (RobotMapping.SWAP_ARM_AXES) axis1 else axis2

        val mappedAxis1 = if (RobotMapping.INVERT_ARM_AXIS_1) {
            -swappedAxis1
        } else {
            swappedAxis1
        }

        val mappedAxis2 = if (RobotMapping.INVERT_ARM_AXIS_2) {
            -swappedAxis2
        } else {
            swappedAxis2
        }

        val safeAxis1 = mappedAxis1.coerceIn(-255, 255)
        val safeAxis2 = mappedAxis2.coerceIn(-255, 255)

        return "A,$safeAxis1,$safeAxis2\n"
    }

    fun stop(): String {
        return "S\n"
    }

    fun autoMove(
        direction: String,
        durationMs: Int,
        speed: Int
    ): String {
        return "AUTO,$direction,${durationMs.coerceAtLeast(0)},${speed.coerceIn(80, 255)}\n"
    }
}