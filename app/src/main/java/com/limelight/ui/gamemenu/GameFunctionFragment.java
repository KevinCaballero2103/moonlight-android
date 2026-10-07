package com.limelight.ui.gamemenu;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;

/**
 * Description
 * Date: 2024-10-20
 * Time: 16:07
 */
public class GameFunctionFragment extends BaseGameMenuDialog implements View.OnClickListener {
    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_function;
    }

    private ImageButton ibtn_back;
    private TextView tx_title;

    private String title;

    @Override
    public void bindView(View v) {
        super.bindView(v);
        ibtn_back=v.findViewById(R.id.ibtn_back);
        tx_title=v.findViewById(R.id.tx_title);

        if(!TextUtils.isEmpty(title)){
            tx_title.setText(title);
        }
        ibtn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        v.findViewById(R.id.btn_task_manager).setOnClickListener(this);
        v.findViewById(R.id.btn_sleep).setOnClickListener(this);
        v.findViewById(R.id.btn_shutdown).setOnClickListener(this);
        v.findViewById(R.id.btn_clipboard_send).setOnClickListener(this);
        v.findViewById(R.id.btn_reboot).setOnClickListener(this);
        v.findViewById(R.id.btn_open_setting).setOnClickListener(this);
        v.findViewById(R.id.btn_logout).setOnClickListener(this);
        v.findViewById(R.id.btn_clipboard_open).setOnClickListener(this);
        v.findViewById(R.id.btn_computer).setOnClickListener(this);
        v.findViewById(R.id.btn_win_center).setOnClickListener(this);
        v.findViewById(R.id.btn_win_p).setOnClickListener(this);
        v.findViewById(R.id.btn_hdr).setOnClickListener(this);

        v.findViewById(R.id.btn_display_1).setOnClickListener(this);
        v.findViewById(R.id.btn_display_2).setOnClickListener(this);
        v.findViewById(R.id.btn_display_3).setOnClickListener(this);
        v.findViewById(R.id.btn_display_4).setOnClickListener(this);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private onClick click;

    @Override
    public void onClick(View v) {
        if(click==null){
            return;
        }
        if(v.getId()==R.id.btn_logout){
            click.click(getString(R.string.axi_ui_cerrar_sesion),0);
            return;
        }
        if(v.getId()==R.id.btn_shutdown){
            click.click(getString(R.string.axi_ui_apagar_pc),1);
            return;
        }
        if(v.getId()==R.id.btn_sleep){
            click.click(getString(R.string.axi_ui_suspender),2);
            return;
        }
        if(v.getId()==R.id.btn_reboot){
            click.click(getString(R.string.axi_ui_reiniciar_pc),3);
            return;
        }
        if(v.getId()==R.id.btn_task_manager){
            click.click(getString(R.string.axi_ui_administrador_de_tareas),4);
            return;
        }
        if(v.getId()==R.id.btn_clipboard_send){
            click.click(getString(R.string.axi_ui_enviar_portapapeles),5);
            return;
        }
        if(v.getId()==R.id.btn_clipboard_open){
            click.click(getString(R.string.axi_ui_abrir_portapapeles),6);
            return;
        }
        if(v.getId()==R.id.btn_open_setting){
            click.click(getString(R.string.axi_ui_abrir_ajustes),7);
            return;
        }
        if(v.getId()==R.id.btn_computer){
            click.click(getString(R.string.axi_ui_este_equipo),8);
            return;
        }
        if(v.getId()==R.id.btn_win_center){
            click.click(getString(R.string.axi_ui_centro_de_movilidad),9);
            return;
        }
        if(v.getId()==R.id.btn_win_p){
            click.click("win+p",10);
            return;
        }

        if(v.getId()==R.id.btn_display_1){
            click.click(getString(R.string.axi_ui_pantalla_1),11);
            return;
        }
        if(v.getId()==R.id.btn_display_2){
            click.click(getString(R.string.axi_ui_pantalla_2),12);
            return;
        }
        if(v.getId()==R.id.btn_display_3){
            click.click(getString(R.string.axi_ui_pantalla_3),13);
            return;
        }
        if(v.getId()==R.id.btn_display_4){
            click.click(getString(R.string.axi_ui_pantalla_4),14);
            return;
        }
        if(v.getId()==R.id.btn_hdr){
            click.click(getString(R.string.axi_ui_activar_desactivar_hdr),15);
            return;
        }

    }

    public interface onClick{
        void click(String title,int index);
    }

    public void setOnClick(onClick onClick) {
        this.click = onClick;
    }
}
