package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.Application;
import android.app.AlertDialog;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import com.google.gson.Gson;
import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, application=Application.class, qualifiers="es", sdk={28,34},
        shadows=KeyboardControlTouchTest.NoNativeMoonBridge.class)
public class SpanishInterfacePresetTest {
    private Activity activity;
    private TouchRoutingLayout root;
    private KeyBoardController controller;
    private View panel, toolbar;
    @Before public void setup() {
        activity=Robolectric.buildActivity(Activity.class).setup().get();
        root=new TouchRoutingLayout(activity);
        controller=new KeyBoardController(null,root,activity,PreferenceConfiguration.readPreferences(activity),false);
        ReflectionHelpers.setField(controller,"fileName","genshin-es-"+System.nanoTime()+".txt");
        controller.currentMode=KeyBoardController.ControllerMode.MoveButtons;
        panel=ReflectionHelpers.getField(controller,"lv_left_view");
        toolbar=ReflectionHelpers.getField(controller,"buttonConfigure");
        root.measure(View.MeasureSpec.makeMeasureSpec(2200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));root.layout(0,0,2200,1080);
    }
    private List<GameMenuQuickBean> beans(){return ReflectionHelpers.getField(controller,"beanList");}
    private void load(){ReflectionHelpers.callInstanceMethod(controller,"loadGenshinPreset");}
    @Test public void allPresetInputsAndGeometryLoadAtTheirTestedCoordinates() throws Exception {
        GameMenuQuickBean[] expected=new Gson().fromJson(new InputStreamReader(activity.getAssets().open("config/genshin_touch_es.json"),StandardCharsets.UTF_8),GameMenuQuickBean[].class);
        load();assertEquals(11,beans().size());
        for(int i=0;i<expected.length;i++) {
            GameMenuQuickBean a=expected[i],b=beans().get(i);
            assertEquals(a.getId(),b.getId());assertEquals(a.getCode(),b.getCode());assertEquals(a.getCodes(),b.getCodes());assertEquals(a.getBtnType(),b.getBtnType());
            assertEquals(a.getWidth(),b.getWidth());assertEquals(a.getHeight(),b.getHeight());assertEquals(a.getmLeft(),b.getmLeft());assertEquals(a.getmTop(),b.getmTop());
            assertEquals(a.getOpacity(),b.getOpacity());assertEquals(a.getStackLevel(),b.getStackLevel());assertEquals(a.isSwitchMode(),b.isSwitchMode());assertEquals(a.getShapeType(),b.getShapeType());
            keyBoardVirtualControllerElement view=root.findViewWithTag(new TagInfo(i,false));assertNotNull(view);assertEquals(b.getStackLevel(),view.getControlLevel());
        }
        assertEquals("Escritorio",beans().get(0).getName());assertEquals("Movimiento (WASD)",beans().get(3).getName());
        assertEquals(1,beans().get(9).getStackLevel());assertEquals(Integer.valueOf(0),beans().get(9).getOpacity());assertEquals("33",beans().get(10).getCodes());
    }
    @Test public void loadingPresetDoesNotReplaceSavedFileUntilSave() throws Exception {
        String name=ReflectionHelpers.getField(controller,"fileName");java.io.File saved=new java.io.File(activity.getFilesDir(),name);
        Files.write(saved.toPath(),"existing design".getBytes(StandardCharsets.UTF_8));
        load();assertEquals("existing design",new String(Files.readAllBytes(saved.toPath()),StandardCharsets.UTF_8));
        ReflectionHelpers.callInstanceMethod(controller,"save");
        GameMenuQuickBean[] savedBeans=new Gson().fromJson(new String(Files.readAllBytes(saved.toPath()),StandardCharsets.UTF_8),GameMenuQuickBean[].class);
        assertEquals(11,savedBeans.length);assertEquals(1,savedBeans[9].getStackLevel());assertEquals(Integer.valueOf(0),savedBeans[9].getOpacity());assertEquals("33",savedBeans[10].getCodes());
    }
    @Test public void cancellingPresetDialogKeepsEditedControls() {
        List<GameMenuQuickBean> old=new ArrayList<>();old.add(new GameMenuQuickBean("Personalizado",13,"personal",2,false));ReflectionHelpers.setField(controller,"beanList",old);
        assertTrue(toolbar.findViewById(R.id.btn_game_virtual_genshin).performClick());
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);
        dialog.findViewById(R.id.btn_app_dialog_secondary).performClick();assertEquals(1,beans().size());assertEquals("Personalizado",beans().get(0).getName());
    }
    @Test public void confirmingPresetDialogLoadsItWithoutSavingAutomatically() {
        toolbar.findViewById(R.id.btn_game_virtual_genshin).performClick();
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(dialog);
        dialog.findViewById(R.id.btn_app_dialog_primary).performClick();assertEquals(11,beans().size());
        String name=ReflectionHelpers.getField(controller,"fileName");assertFalse(new java.io.File(activity.getFilesDir(),name).exists());
    }
    @Test public void editorAndNewModesResolveSpanishResources() {
        load();ReflectionHelpers.callInstanceMethod(controller,"updateItem",ReflectionHelpers.ClassParameter.from(int.class,8));
        assertEquals("Forma rectangular",((CheckBox)panel.findViewById(R.id.cb_round)).getText().toString());
        assertTrue(((TextView)panel.findViewById(R.id.tx_zoom)).getText().toString().startsWith("Diámetro:"));
        assertTrue(((TextView)panel.findViewById(R.id.tx_stack_level)).getText().toString().startsWith("Nivel:"));
        assertEquals("Cámara + toque",activity.getString(R.string.control_camera_tap));assertEquals("Tecla + movimiento",activity.getString(R.string.control_key_camera));
        assertTrue(((SeekBar)panel.findViewById(R.id.sb_zoom_x)).getMax()>=1000);
        assertEquals("Guardar diseño",((TextView)toolbar.findViewById(R.id.btn_game_virtual_save)).getText().toString());
    }
    @Test public void gamepadEditorDoesNotOfferKeyboardPreset() {
        KeyBoardController gamepad=new KeyBoardController(null,new TouchRoutingLayout(activity),activity,PreferenceConfiguration.readPreferences(activity),true);
        View tools=ReflectionHelpers.getField(gamepad,"buttonConfigure");assertEquals(View.GONE,tools.findViewById(R.id.btn_game_virtual_genshin).getVisibility());
    }
}
