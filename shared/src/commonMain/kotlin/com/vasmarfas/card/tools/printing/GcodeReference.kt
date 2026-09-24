package com.vasmarfas.card.tools.printing

import com.vasmarfas.card.resources.*
import org.jetbrains.compose.resources.StringResource

class GcodeEntry(
    val code: String,
    val params: String,
    val flavor: String,
    val description: StringResource,
)

object GcodeReference {
    val entries: List<GcodeEntry> = listOf(
        GcodeEntry(
            "G0 / G1", "X Y Z E F", "Marlin · Klipper",
            Res.string.gcode_linear_move_g0_travel,
        ),
        GcodeEntry(
            "G2 / G3", "X Y I J R F", "Marlin · Klipper",
            Res.string.gcode_clockwise_counter_clockwise,
        ),
        GcodeEntry(
            "G4", "P S", "Marlin · Klipper",
            Res.string.gcode_dwell_pause_for_p,
        ),
        GcodeEntry(
            "G10 / G11", "", "Marlin",
            Res.string.gcode_firmware_retract_and_recover,
        ),
        GcodeEntry(
            "G28", "X Y Z", "Marlin · Klipper",
            Res.string.gcode_home_the_listed_axes,
        ),
        GcodeEntry(
            "G29", "", "Marlin",
            Res.string.gcode_run_bed_levelling,
        ),
        GcodeEntry(
            "G90 / G91", "", "Marlin · Klipper",
            Res.string.gcode_absolute_relative,
        ),
        GcodeEntry(
            "G92", "X Y Z E", "Marlin · Klipper",
            Res.string.gcode_set_the_current_position,
        ),
        GcodeEntry(
            "M17 / M18", "X Y Z E", "Marlin",
            Res.string.gcode_enable_disable_stepper,
        ),
        GcodeEntry(
            "M82 / M83", "", "Marlin · Klipper",
            Res.string.gcode_absolute_relative_extrusion,
        ),
        GcodeEntry(
            "M84", "S", "Marlin · Klipper",
            Res.string.gcode_disable_steppers_s_sets,
        ),
        GcodeEntry(
            "M92", "X Y Z E", "Marlin",
            Res.string.gcode_set_steps_per_millimeter,
        ),
        GcodeEntry(
            "M104", "S T", "Marlin · Klipper",
            Res.string.gcode_set_the_hotend_temperature,
        ),
        GcodeEntry(
            "M105", "", "Marlin · Klipper",
            Res.string.gcode_report_current_hotend,
        ),
        GcodeEntry(
            "M106 / M107", "S P", "Marlin · Klipper",
            Res.string.gcode_part_cooling_fan,
        ),
        GcodeEntry(
            "M109", "S R", "Marlin · Klipper",
            Res.string.gcode_set_the_hotend_temperature_and,
        ),
        GcodeEntry(
            "M112", "", "Marlin · Klipper",
            Res.string.gcode_emergency_stop_shuts_down,
        ),
        GcodeEntry(
            "M114", "", "Marlin · Klipper",
            Res.string.report_the_current_xyze_position,
        ),
        GcodeEntry(
            "M115", "", "Marlin · Klipper",
            Res.string.gcode_report_firmware_version,
        ),
        GcodeEntry(
            "M117", "", "Marlin",
            Res.string.gcode_show_a_message,
        ),
        GcodeEntry(
            "M140", "S", "Marlin · Klipper",
            Res.string.gcode_set_the_bed_temperature,
        ),
        GcodeEntry(
            "M190", "S R", "Marlin · Klipper",
            Res.string.gcode_set_the_bed_temperature_and,
        ),
        GcodeEntry(
            "M201 / M204", "X Y Z E P T", "Marlin",
            Res.string.gcode_maximum_acceleration,
        ),
        GcodeEntry(
            "M203", "X Y Z E", "Marlin",
            Res.string.maximum_feedrate_per_axis_in_mm_s,
        ),
        GcodeEntry(
            "M205", "X Y Z E J", "Marlin",
            Res.string.gcode_jerk_per_axis,
        ),
        GcodeEntry(
            "M206", "X Y Z", "Marlin",
            Res.string.home_offset_applied_after_g28,
        ),
        GcodeEntry(
            "M220", "S", "Marlin · Klipper",
            Res.string.speed_override_in_percent,
        ),
        GcodeEntry(
            "M221", "S", "Marlin · Klipper",
            Res.string.gcode_flow_extrusion_override,
        ),
        GcodeEntry(
            "M300", "S P", "Marlin",
            Res.string.gcode_beep_at_frequency,
        ),
        GcodeEntry(
            "M301", "P I D", "Marlin",
            Res.string.set_hotend_pid_coefficients,
        ),
        GcodeEntry(
            "M303", "E S C U", "Marlin",
            Res.string.gcode_pid_autotune_e0_hotend,
        ),
        GcodeEntry(
            "M500 / M501 / M502", "", "Marlin",
            Res.string.gcode_save_settings_to_eeprom,
        ),
        GcodeEntry(
            "M503", "", "Marlin",
            Res.string.gcode_print_the_current_settings,
        ),
        GcodeEntry(
            "M600", "X Y Z E L", "Marlin",
            Res.string.gcode_filament_change_park_unload,
        ),
        GcodeEntry(
            "M605", "S", "Marlin",
            Res.string.gcode_idex_mode_full_control,
        ),
        GcodeEntry(
            "M851", "Z", "Marlin",
            Res.string.gcode_z_probe_offset_relative,
        ),
        GcodeEntry(
            "M900", "K", "Marlin",
            Res.string.gcode_linear_advance_factor_k,
        ),
        GcodeEntry(
            "SET_PRESSURE_ADVANCE", "ADVANCE SMOOTH_TIME", "Klipper",
            Res.string.gcode_set_pressure_advance,
        ),
        GcodeEntry(
            "BED_MESH_CALIBRATE", "PROFILE METHOD", "Klipper",
            Res.string.gcode_probe_the_bed,
        ),
        GcodeEntry(
            "BED_MESH_PROFILE", "LOAD SAVE REMOVE", "Klipper",
            Res.string.gcode_load_save_or_delete,
        ),
        GcodeEntry(
            "PROBE_CALIBRATE", "", "Klipper",
            Res.string.gcode_interactive_probe_z_offset,
        ),
        GcodeEntry(
            "SCREWS_TILT_CALCULATE", "", "Klipper",
            Res.string.gcode_measure_the_bed_corners,
        ),
        GcodeEntry(
            "QUAD_GANTRY_LEVEL", "", "Klipper",
            Res.string.gcode_level_a_four_motor,
        ),
        GcodeEntry(
            "Z_TILT_ADJUST", "", "Klipper",
            Res.string.gcode_level_the_bed,
        ),
        GcodeEntry(
            "TUNING_TOWER", "COMMAND PARAMETER START FACTOR BAND", "Klipper",
            Res.string.gcode_sweep_a_parameter_over,
        ),
        GcodeEntry(
            "SET_VELOCITY_LIMIT", "VELOCITY ACCEL SQUARE_CORNER_VELOCITY", "Klipper",
            Res.string.gcode_temporarily_override_speed,
        ),
        GcodeEntry(
            "SET_HEATER_TEMPERATURE", "HEATER TARGET", "Klipper",
            Res.string.gcode_set_any_heater,
        ),
        GcodeEntry(
            "FIRMWARE_RESTART", "", "Klipper",
            Res.string.gcode_restart_the_firmware,
        ),
        GcodeEntry(
            "SAVE_CONFIG", "", "Klipper",
            Res.string.gcode_write_the_values_calibration,
        ),
        GcodeEntry(
            "PAUSE / RESUME / CANCEL_PRINT", "", "Klipper",
            Res.string.gcode_pause_resume_or_cancel,
        ),
    )

    fun search(query: String): List<GcodeEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries
        return entries.filter {
            it.code.lowercase().contains(q) ||
                it.params.lowercase().contains(q) ||
                it.flavor.lowercase().contains(q) ||
                it.description.matches(q)
        }
    }
}
