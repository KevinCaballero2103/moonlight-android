package com.limelight.preferences;

import com.limelight.R;

/** Navigation for the persistent settings sheet; independent of the in-stream quick menu. */
final class SettingsCatalog {
    static final class Section {
        final int title;
        final String[] keys;

        Section(int title, String... keys) {
            this.title = title;
            this.keys = keys;
        }
    }

    static final class Group {
        final int title;
        final int description;
        final Section[] sections;

        Group(int title, int description, Section... sections) {
            this.title = title;
            this.description = description;
            this.sections = sections;
        }
    }

    static final Group[] GROUPS = {
        new Group(R.string.axi_ui_pantalla, R.string.axi_ui_resolucion_fps_y_mejora_de_imagen,
            new Section(R.string.axi_ui_parametros_de_video, "list_resolution", "list_fps", "seekbar_bitrate_kbps", "video_format", "vdValue"),
            new Section(R.string.axi_ui_encuadre_de_imagen, "checkbox_stretch_video", "checkbox_cutout_mode_video",
                "checkbox_auto_screen_orientation", "checkbox_enable_portrait", "screen_gravity_list",
                "checkbox_keep_video_zoom_on_disable"),
            new Section(R.string.axi_ui_mejora_de_imagen, "list_video_render_mode", "list_fsr_target", "list_fsr_sharpness",
                "list_fsr_hdr_output", "list_stereo_3d_mode", "list_stereo_3d_depth",
                "list_stereo_3d_convergence", "checkbox_stereo_3d_swap_eyes", "checkbox_enable_hdr",
                "checkbox_enable_hdr_high_brightness", "checkbox_full_range"),
            new Section(R.string.axi_ui_compatibilidad_y_rendimiento, "frame_pacing", "enable_lowLatency_experiment",
                "checkbox_enable_xiaomi_xring_o1_optimization", "checkbox_unlock_fps",
                "checkbox_reduce_refresh_rate", "checkbox_enforce_display_mode", "checkbox_enable_sops")),
        new Group(R.string.axi_ui_mando, R.string.axi_ui_conexion_asignacion_giroscopio_y_vibracion,
            new Section(R.string.axi_ui_conexion_y_asignacion, "list_gamepad_emulation", "checkbox_multi_controller", "seekbar_deadzone",
                "checkbox_disable_trigger_deadzone", "checkbox_flip_face_buttons", "checkbox_usb_driver",
                "checkbox_usb_bind_all", "checkbox_enable_joyconfix", "checkbox_gamepad_enable_battery_report"),
            new Section(R.string.axi_ui_raton_y_giroscopio, "checkbox_mouse_emulation", "analog_scrolling",
                "checkbox_gamepad_touchpad_as_mouse", "checkbox_gamepad_motion_sensors",
                "checkbox_gamepad_motion_fallback", "checkbox_enable_virtual_motion"),
            new Section(R.string.axi_ui_vibracion_y_dualsense, "checkbox_flip_rumble_ff", "checkbox_enable_device_rumble",
                "checkbox_vibrate_fallback", "seekbar_vibrate_fallback_strength",
                "checkbox_ds5_native_pcm", "checkbox_ds5_controller_speaker")),
        new Group(R.string.axi_ui_tactil_y_raton, R.string.axi_ui_pantalla_tactil_raton_y_teclado_fisico,
            new Section(R.string.axi_ui_raton_y_pantalla_tactil, "mouse_model_list_axi", "checkbox_touch_stutter_compatibility", "checkbox_mouse_local_cursor",
                "checkbox_mouse_nav_buttons", "checkbox_absolute_mouse_mode"),
            new Section(R.string.axi_ui_teclado_fisico_y_accesibilidad, "checkbox_keyboard_esc_opens_game_menu",
                "checkbox_enable_clear_default_special_button", "import_switch_button_file")),
        new Group(R.string.axi_ui_sonido, R.string.axi_ui_canales_y_destino_del_audio,
            new Section(R.string.axi_ui_salida_de_audio, "list_audio_config", "checkbox_enable_audiofx", "checkbox_host_audio")),
        new Group(R.string.axi_ui_vibracion_por_audio, R.string.axi_ui_destino_intensidad_y_filtro_de_voz,
            new Section(R.string.axi_ui_vibracion_por_audio, "checkbox_enable_audio_haptics", "list_audio_haptics_output_target",
                "seekbar_audio_haptics_strength", "list_audio_haptics_voice_filter",
                "checkbox_audio_haptics_keep_controller_rumble")),
        new Group(R.string.axi_ui_controles_virtuales, R.string.axi_ui_controles_en_pantalla_y_teclado_completo,
            new Section(R.string.axi_ui_controles_en_pantalla, "checkbox_enable_keyboard", "keyboard_axi_list", "checkbox_vibrate_keyboard"),
            new Section(R.string.axi_ui_teclado_completo_c1b36562, "seekbar_keyboard_axi_opacity", "seekbar_keyboard_axi_height",
                "checkbox_enable_keyboard_axi_combination"),
            new Section(R.string.axi_ui_layouts_de_controles, "import_keyboard_file", "export_keyboard_file")),
        new Group(R.string.axi_ui_mando_virtual, R.string.axi_ui_layout_opacidad_y_vibracion,
            new Section(R.string.axi_ui_mando_en_pantalla, "checkbox_show_onscreen_controls", "gamepad_axi_list", "seekbar_osc_opacity",
                "checkbox_vibrate_osc", "checkbox_rocker_click_L3R3"),
            new Section(R.string.axi_ui_layouts_del_mando, "import_gamepad_file", "export_gamepad_file")),
        new Group(R.string.axi_ui_informacion_en_pantalla, R.string.axi_ui_rendimiento_y_boton_flotante,
            new Section(R.string.axi_ui_informacion_de_rendimiento, "performance_overlay_mode", "list_perf_overlay_lite_position",
                "checkbox_enable_perf_overlay_lite_dialog",
                "checkbox_enable_perf_overlay_lite_ext", "performance_overlayLite_magin_top",
                "checkbox_enable_post_stream_toast"),
            new Section(R.string.axi_ui_boton_flotante, "checkbox_enable_ax_floating")),
        new Group(R.string.axi_ui_copias_de_seguridad, R.string.axi_ui_exportar_o_importar_equipos_emparejados,
            new Section(R.string.axi_ui_equipos_y_datos_de_emparejamiento, "export_pairing_backup", "import_pairing_backup")),
        new Group(R.string.axi_ui_recopilacion_de_registros, R.string.axi_ui_registrar_sesiones_y_administrar_archivos,
            new Section(R.string.axi_ui_registros_de_streaming, "checkbox_enable_stream_session_logging", "manage_stream_session_logs"),
            new Section(R.string.axi_ui_depuracion_de_entrada, "checkbox_enable_accessibility_show_log")),
        new Group(R.string.axi_ui_interfaz_y_general, R.string.axi_ui_idioma_inicio_y_comportamiento_de_la_app,
            new Section(R.string.axi_ui_interfaz, "list_languages", "change_screen_label_key",
                "checkbox_enable_screen_bg", "import_image_file_key", "checkbox_enable_screen_obscure"),
            new Section(R.string.axi_ui_comportamiento_de_la_app, "checkbox_enable_pip", "checkbox_enable_exdisplay",
                "checkbox_enable_game_manager_quest", "checkbox_disable_warnings"))
    };

    // These legacy rows are represented by one editor, or no longer apply to the new surface.
    static final String[] MERGED_OR_RETIRED = {
        "edit_diy_w_h", "edit_diy_bitrate", "checkbox_enable_perf_overlay",
        "checkbox_enable_perf_overlay_lite", "checkbox_ui_theme_white",
        "checkbox_small_icon_mode", "checkbox_enable_pass_menu", "settings_about", "settings_help"
    };

    static int overlayMode(boolean enabled, boolean lite) {
        return !enabled ? 0 : lite ? 1 : 2;
    }

    private SettingsCatalog() {}
}
