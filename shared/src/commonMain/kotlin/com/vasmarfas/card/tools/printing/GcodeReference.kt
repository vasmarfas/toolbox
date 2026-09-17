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
            Res.string.linear_move_g0_travel_g1_print_e_is_the_extr,
        ),
        GcodeEntry(
            "G2 / G3", "X Y I J R F", "Marlin · Klipper",
            Res.string.clockwise_counter_clockwise_arc_move_either,
        ),
        GcodeEntry(
            "G4", "P S", "Marlin · Klipper",
            Res.string.dwell_pause_for_p_milliseconds_or_s_seconds,
        ),
        GcodeEntry(
            "G10 / G11", "", "Marlin",
            Res.string.firmware_retract_and_recover_using_the_m207,
        ),
        GcodeEntry(
            "G28", "X Y Z", "Marlin · Klipper",
            Res.string.home_the_listed_axes_with_no_arguments_homes,
        ),
        GcodeEntry(
            "G29", "", "Marlin",
            Res.string.run_bed_levelling_and_build_the_mesh_abl_ubl,
        ),
        GcodeEntry(
            "G90 / G91", "", "Marlin · Klipper",
            Res.string.absolute_relative_positioning_for_all_axes,
        ),
        GcodeEntry(
            "G92", "X Y Z E", "Marlin · Klipper",
            Res.string.set_the_current_position_without_moving_g92,
        ),
        GcodeEntry(
            "M17 / M18", "X Y Z E", "Marlin",
            Res.string.enable_disable_stepper_drivers_on_the_listed,
        ),
        GcodeEntry(
            "M82 / M83", "", "Marlin · Klipper",
            Res.string.absolute_relative_extrusion_mode_for_the_e_a,
        ),
        GcodeEntry(
            "M84", "S", "Marlin · Klipper",
            Res.string.disable_steppers_s_sets_the_idle_timeout_in,
        ),
        GcodeEntry(
            "M92", "X Y Z E", "Marlin",
            Res.string.set_steps_per_millimetre_for_each_axis_store,
        ),
        GcodeEntry(
            "M104", "S T", "Marlin · Klipper",
            Res.string.set_the_hotend_temperature_and_continue_with,
        ),
        GcodeEntry(
            "M105", "", "Marlin · Klipper",
            Res.string.report_current_hotend_and_bed_temperatures,
        ),
        GcodeEntry(
            "M106 / M107", "S P", "Marlin · Klipper",
            Res.string.part_cooling_fan_on_with_speed_s_0_255_fan_o,
        ),
        GcodeEntry(
            "M109", "S R", "Marlin · Klipper",
            Res.string.set_the_hotend_temperature_and_wait_s_waits,
        ),
        GcodeEntry(
            "M112", "", "Marlin · Klipper",
            Res.string.emergency_stop_shuts_down_heaters_and_motors,
        ),
        GcodeEntry(
            "M114", "", "Marlin · Klipper",
            Res.string.report_the_current_xyze_position,
        ),
        GcodeEntry(
            "M115", "", "Marlin · Klipper",
            Res.string.report_firmware_version_and_capabilities,
        ),
        GcodeEntry(
            "M117", "", "Marlin",
            Res.string.show_a_message_on_the_printer_display,
        ),
        GcodeEntry(
            "M140", "S", "Marlin · Klipper",
            Res.string.set_the_bed_temperature_without_waiting,
        ),
        GcodeEntry(
            "M190", "S R", "Marlin · Klipper",
            Res.string.set_the_bed_temperature_and_wait_for_it,
        ),
        GcodeEntry(
            "M201 / M204", "X Y Z E P T", "Marlin",
            Res.string.maximum_acceleration_per_axis_print_and_trav,
        ),
        GcodeEntry(
            "M203", "X Y Z E", "Marlin",
            Res.string.maximum_feedrate_per_axis_in_mm_s,
        ),
        GcodeEntry(
            "M205", "X Y Z E J", "Marlin",
            Res.string.jerk_per_axis_or_junction_deviation_with_j,
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
            Res.string.flow_extrusion_override_in_percent,
        ),
        GcodeEntry(
            "M300", "S P", "Marlin",
            Res.string.beep_at_frequency_s_for_p_milliseconds,
        ),
        GcodeEntry(
            "M301", "P I D", "Marlin",
            Res.string.set_hotend_pid_coefficients,
        ),
        GcodeEntry(
            "M303", "E S C U", "Marlin",
            Res.string.pid_autotune_e0_hotend_e_1_bed_s_target_c_cy,
        ),
        GcodeEntry(
            "M500 / M501 / M502", "", "Marlin",
            Res.string.save_settings_to_eeprom_load_them_reset_to_f,
        ),
        GcodeEntry(
            "M503", "", "Marlin",
            Res.string.print_the_current_settings_as_g_code,
        ),
        GcodeEntry(
            "M600", "X Y Z E L", "Marlin",
            Res.string.filament_change_park_unload_and_wait_for_the,
        ),
        GcodeEntry(
            "M605", "S", "Marlin",
            Res.string.idex_mode_full_control_duplication_or_mirror,
        ),
        GcodeEntry(
            "M851", "Z", "Marlin",
            Res.string.z_probe_offset_relative_to_the_nozzle_negati,
        ),
        GcodeEntry(
            "M900", "K", "Marlin",
            Res.string.linear_advance_factor_k_the_marlin_equivalen,
        ),
        GcodeEntry(
            "SET_PRESSURE_ADVANCE", "ADVANCE SMOOTH_TIME", "Klipper",
            Res.string.set_pressure_advance_for_the_active_extruder,
        ),
        GcodeEntry(
            "BED_MESH_CALIBRATE", "PROFILE METHOD", "Klipper",
            Res.string.probe_the_bed_and_build_a_mesh_save_config_s,
        ),
        GcodeEntry(
            "BED_MESH_PROFILE", "LOAD SAVE REMOVE", "Klipper",
            Res.string.load_save_or_delete_a_stored_bed_mesh_profil,
        ),
        GcodeEntry(
            "PROBE_CALIBRATE", "", "Klipper",
            Res.string.interactive_probe_z_offset_calibration_using,
        ),
        GcodeEntry(
            "SCREWS_TILT_CALCULATE", "", "Klipper",
            Res.string.measure_the_bed_corners_and_report_how_far_t,
        ),
        GcodeEntry(
            "QUAD_GANTRY_LEVEL", "", "Klipper",
            Res.string.level_a_four_motor_gantry_voron_style_by_pro,
        ),
        GcodeEntry(
            "Z_TILT_ADJUST", "", "Klipper",
            Res.string.level_the_bed_with_independent_z_motors,
        ),
        GcodeEntry(
            "TUNING_TOWER", "COMMAND PARAMETER START FACTOR BAND", "Klipper",
            Res.string.sweep_a_parameter_over_z_height_the_standard,
        ),
        GcodeEntry(
            "SET_VELOCITY_LIMIT", "VELOCITY ACCEL SQUARE_CORNER_VELOCITY", "Klipper",
            Res.string.temporarily_override_speed_acceleration_and,
        ),
        GcodeEntry(
            "SET_HEATER_TEMPERATURE", "HEATER TARGET", "Klipper",
            Res.string.set_any_heater_by_name_including_a_chamber_h,
        ),
        GcodeEntry(
            "FIRMWARE_RESTART", "", "Klipper",
            Res.string.restart_the_firmware_and_clear_a_shutdown_st,
        ),
        GcodeEntry(
            "SAVE_CONFIG", "", "Klipper",
            Res.string.write_the_values_calibration_produced_into_p,
        ),
        GcodeEntry(
            "PAUSE / RESUME / CANCEL_PRINT", "", "Klipper",
            Res.string.pause_resume_or_cancel_the_current_print_mac,
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
