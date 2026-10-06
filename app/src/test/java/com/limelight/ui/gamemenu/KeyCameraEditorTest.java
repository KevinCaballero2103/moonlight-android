package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.app.Application;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RadioGroup;
import com.limelight.R;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardTouchPadButton;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, application = Application.class, sdk = {28, 34})
public class KeyCameraEditorTest {
    private Activity activity;
    private GameKeyboardUpdateFragment editor;
    private View view;
    private final List<GameMenuQuickBean> saved = new ArrayList<>();
    @Before public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        editor = new GameKeyboardUpdateFragment();
        editor.setOnClick(saved::add);
        editor.show(activity.getFragmentManager());
        activity.getFragmentManager().executePendingTransactions();
        view = editor.getView();
    }
    private View key(View node, String code) {
        if (code.equals(node.getTag())) return node;
        if (node instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) node;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = key(group.getChildAt(i), code);
                if (found != null) return found;
            }
        }
        return null;
    }
    private void select(String code) {
        View key = key(view.findViewById(R.id.lv_keyboard), code);
        assertNotNull(key);
        MotionEvent down = MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,1,1,0);
        MotionEvent up = MotionEvent.obtain(0,10,MotionEvent.ACTION_UP,1,1,0);
        try { assertTrue(key.dispatchTouchEvent(down)); assertTrue(key.dispatchTouchEvent(up)); }
        finally { down.recycle(); up.recycle(); }
    }
    @Test public void selectingAimFromMouseListStartsWithEThenAllowsReplacement() {
        GridView grid = view.findViewById(R.id.rv_keyboard_mouse);
        ((RadioGroup)view.findViewById(R.id.rg_keyboard)).check(R.id.rbt_keyboard_3);
        int index = -1;
        for (int i = 0; i < grid.getAdapter().getCount(); i++) {
            GameMenuQuickBean item = (GameMenuQuickBean) grid.getAdapter().getItem(i);
            if (item.getBtnType() == 2 && item.getCode() == KeyBoardTouchPadButton.CODE_KEY_CAMERA) index = i;
        }
        assertTrue(index >= 0);
        grid.performItemClick(null, index, index);
        assertTrue(saved.isEmpty());
        assertTrue(((CheckBox)view.findViewById(R.id.cb_key_camera)).isChecked());
        assertEquals("E", ((EditText)view.findViewById(R.id.edt_name)).getText().toString());
        select("34"); // F replaces E
        view.findViewById(R.id.btn_right).performClick();
        assertEquals(1, saved.size());
        assertEquals("34", saved.get(0).getCodes());
        assertEquals(15, saved.get(0).getCode());
        assertEquals(2, saved.get(0).getBtnType());
        assertEquals(1, saved.get(0).getShapeType());
        assertFalse(saved.get(0).isSwitchMode());
    }
    @Test public void ordinaryKeyboardSelectionStillCreatesACombination() {
        select("113");
        select("8");
        view.findViewById(R.id.btn_right).performClick();
        assertEquals(1, saved.size());
        assertEquals(4, saved.get(0).getBtnType());
        assertEquals("113,8", saved.get(0).getCodes());
    }
    @Test public void turningAimOnClearsAChordAndSelectingReplacesOneKey() {
        select("113"); select("33");
        ((CheckBox)view.findViewById(R.id.cb_key_camera)).setChecked(true);
        assertEquals("", ((EditText)view.findViewById(R.id.edt_name)).getText().toString());
        select("33"); select("34");
        view.findViewById(R.id.btn_right).performClick();
        assertEquals("34", saved.get(0).getCodes());
        assertEquals(2, saved.get(0).getBtnType());
    }
}
