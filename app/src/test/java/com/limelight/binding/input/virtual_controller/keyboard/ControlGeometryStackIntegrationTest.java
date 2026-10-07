package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.Application;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.TouchRoutingLayout;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, application=Application.class, sdk={28,34},
        shadows=KeyboardControlTouchTest.NoNativeMoonBridge.class)
public class ControlGeometryStackIntegrationTest {
    private Activity activity;
    private TouchRoutingLayout root;
    private KeyBoardController controller;
    private List<GameMenuQuickBean> beans;
    private View panel;
    private final List<String> events=new ArrayList<>();
    private long time=1000;
    @Before public void setup() {
        activity=Robolectric.buildActivity(Activity.class).setup().get();
        root=new TouchRoutingLayout(activity);
        controller=new KeyBoardController(null,root,activity,PreferenceConfiguration.readPreferences(activity),false);
        controller.currentMode=KeyBoardController.ControllerMode.MoveButtons;
        beans=new ArrayList<>();ReflectionHelpers.setField(controller,"beanList",beans);
        panel=ReflectionHelpers.getField(controller,"lv_left_view");
    }
    private GameMenuQuickBean bean(int type,int code,int w,int h) {
        GameMenuQuickBean b=new GameMenuQuickBean("control",code,"control",type,false);
        b.setWidth(w);b.setHeight(h);b.setShapeType(1);b.setmLeft(200);b.setmTop(100);
        beans.add(b);return b;
    }
    private keyBoardVirtualControllerElement load(int index) {
        ReflectionHelpers.callInstanceMethod(controller,"addView",ReflectionHelpers.ClassParameter.from(GameMenuQuickBean.class,beans.get(index)),ReflectionHelpers.ClassParameter.from(int.class,index));
        layout();return root.findViewWithTag(new TagInfo(index,false));
    }
    private void select(int index) {
        ReflectionHelpers.callInstanceMethod(controller,"updateItem",ReflectionHelpers.ClassParameter.from(int.class,index));
    }
    private void change(int id,int value) {
        SeekBar bar=panel.findViewById(id);
        SeekBar.OnSeekBarChangeListener listener=ReflectionHelpers.getField(bar,"mOnSeekBarChangeListener");
        listener.onProgressChanged(bar,value,true);layout();
    }
    private void layout() {
        root.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));root.layout(0,0,1000,600);
    }
    private void capture(KeyBoardTouchPadButton pad,String name) {
        pad.addDigitalButtonListener(new KeyBoardTouchPadButton.DigitalButtonListener() {
            @Override public void onClick(){events.add(name+":down");}
            @Override public void onRelease(){events.add(name+":up");}
            @Override public void onLongClick(){}
            @Override public void onMove(int x,int y){events.add(name+":move");}
        });
    }
    private void event(int action,int index,int[] ids,float... xy) {
        MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] c=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){p[i]=new MotionEvent.PointerProperties();p[i].id=ids[i];p[i].toolType=MotionEvent.TOOL_TYPE_FINGER;c[i]=new MotionEvent.PointerCoords();c[i].x=xy[i*2];c[i].y=xy[i*2+1];c[i].pressure=1;}
        MotionEvent e=MotionEvent.obtain(1000,time+=10,action|index<<MotionEvent.ACTION_POINTER_INDEX_SHIFT,ids.length,p,c,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        try{assertTrue(root.dispatchTouchEvent(e));}finally{e.recycle();}
    }
    private void tap(float x,float y){event(MotionEvent.ACTION_DOWN,0,new int[]{7},x,y);event(MotionEvent.ACTION_UP,0,new int[]{7},x,y);}
    @Test public void editorUsesDiameterAndRecoversRectangleAtSameCenter() {
        for(int type:new int[]{1,2,4,6}) {
            // Use mouse/region pads with no JNI keyboard keys in this fixture.
            GameMenuQuickBean b=bean(type,1,400,200);if(type==4)b.setCodes("33");
            keyBoardVirtualControllerElement view=load(beans.size()-1);select(beans.size()-1);
            CheckBox shape=panel.findViewById(R.id.cb_round);shape.setChecked(false);layout();
            assertEquals(View.GONE,panel.findViewById(R.id.lv_zoom_wh).getVisibility());
            assertEquals(200,view.getWidth());assertEquals(200,view.getHeight());assertEquals(300,((FrameLayout.LayoutParams)view.getLayoutParams()).leftMargin);
            change(R.id.sb_zoom_x,150);assertEquals(view.getWidth(),view.getHeight());
            assertTrue(((TextView)panel.findViewById(R.id.tx_zoom)).getText().toString().contains("150"));
            shape.setChecked(true);layout();assertEquals(400,view.getWidth());assertEquals(200,view.getHeight());
            assertEquals(200,((FrameLayout.LayoutParams)view.getLayoutParams()).leftMargin);assertEquals(100,((FrameLayout.LayoutParams)view.getLayoutParams()).topMargin);
            assertEquals(View.VISIBLE,panel.findViewById(R.id.lv_zoom_wh).getVisibility());
        }
    }
    @Test public void selectingDifferentShapesDoesNotModifyEitherImportedSize() {
        GameMenuQuickBean a=bean(2,13,417,193);GameMenuQuickBean b=bean(2,14,200,200);b.setShapeType(0);
        load(0);load(1);select(0);select(1);select(0);
        assertEquals(417,a.getWidth());assertEquals(193,a.getHeight());assertEquals(200,b.getWidth());assertEquals(200,b.getHeight());
    }
    @Test public void rectangleSlidersChangeOneDimensionOnlyAndCircleIgnoresHiddenSliders() {
        GameMenuQuickBean b=bean(2,13,417,193);keyBoardVirtualControllerElement v=load(0);select(0);
        change(R.id.sb_zoom_w,150);assertEquals(193,v.getHeight());
        int width=v.getWidth();change(R.id.sb_zoom_h,180);assertEquals(width,v.getWidth());
        ((CheckBox)panel.findViewById(R.id.cb_round)).setChecked(false);layout();int d=v.getWidth();
        change(R.id.sb_zoom_w,50);change(R.id.sb_zoom_h,50);assertEquals(d,v.getWidth());assertEquals(d,v.getHeight());
    }
    @Test public void newerLargePadBelowOlderButtonInDrawingAndHitTest() {
        GameMenuQuickBean a=bean(2,14,100,100);a.setStackLevel(2);
        KeyBoardTouchPadButton attack=(KeyBoardTouchPadButton)load(0);capture(attack,"attack");
        GameMenuQuickBean b=bean(2,14,400,300);b.setStackLevel(1);
        KeyBoardTouchPadButton camera=(KeyBoardTouchPadButton)load(1);capture(camera,"camera");
        assertTrue(attack.getZ()>camera.getZ());controller.currentMode=KeyBoardController.ControllerMode.Active;
        tap(250,150);assertEquals(List.of("attack:down","attack:up"),events);
    }
    @Test public void levelEditorUpdatesRealHitOrderAndSurvivesRebuild() {
        bean(2,14,100,100);KeyBoardTouchPadButton older=(KeyBoardTouchPadButton)load(0);capture(older,"older");
        bean(2,14,100,100);KeyBoardTouchPadButton newer=(KeyBoardTouchPadButton)load(1);capture(newer,"newer");
        select(0);change(R.id.sb_stack_level,4);assertEquals(5,beans.get(0).getStackLevel());assertEquals(5f,older.getZ(),0);
        controller.currentMode=KeyBoardController.ControllerMode.Active;tap(250,150);assertEquals(List.of("older:down","older:up"),events);
        controller.currentMode=KeyBoardController.ControllerMode.MoveButtons;
        ReflectionHelpers.callInstanceMethod(controller,"updateItem");layout();
        keyBoardVirtualControllerElement rebuilt=root.findViewWithTag(new TagInfo(0,false));assertEquals(5f,rebuilt.getZ(),0);
        assertEquals(2,beans.get(1).getStackLevel());
        View editor=ReflectionHelpers.getField(controller,"buttonConfigure");assertTrue(editor.getZ()>rebuilt.getZ());
    }
    @Test public void plusMinusButtonsSelectExactLayersAndClampAtEdges() {
        bean(2,13,100,100);keyBoardVirtualControllerElement v=load(0);select(0);
        panel.findViewById(R.id.btn_stack_down).performClick();assertEquals(1,beans.get(0).getStackLevel());
        panel.findViewById(R.id.btn_stack_down).performClick();assertEquals(1,beans.get(0).getStackLevel());
        panel.findViewById(R.id.btn_stack_up).performClick();assertEquals(2,beans.get(0).getStackLevel());assertEquals(2f,v.getZ(),0);
        change(R.id.sb_stack_level,98);panel.findViewById(R.id.btn_stack_up).performClick();assertEquals(99,beans.get(0).getStackLevel());
    }
    @Test public void equalLevelsKeepNewestOnTop() {
        bean(2,14,100,100);capture((KeyBoardTouchPadButton)load(0),"older");
        bean(2,14,100,100);capture((KeyBoardTouchPadButton)load(1),"newer");
        controller.currentMode=KeyBoardController.ControllerMode.Active;tap(250,150);assertEquals(List.of("newer:down","newer:up"),events);
    }
    @Test public void circularTopPadCornerFallsThroughToLowerPad() {
        GameMenuQuickBean a=bean(2,14,400,300);a.setStackLevel(1);capture((KeyBoardTouchPadButton)load(0),"lower");
        GameMenuQuickBean b=bean(2,14,100,100);b.setShapeType(0);b.setStackLevel(2);capture((KeyBoardTouchPadButton)load(1),"round");
        controller.currentMode=KeyBoardController.ControllerMode.Active;tap(202,102);assertEquals(List.of("lower:down","lower:up"),events);events.clear();
        tap(250,150);assertEquals(List.of("round:down","round:up"),events);
    }
    @Test public void secondFingerKeepsCaptureWhileOverlapHitsHigherButton() {
        GameMenuQuickBean a=bean(2,14,100,100);a.setStackLevel(2);capture((KeyBoardTouchPadButton)load(0),"button");
        GameMenuQuickBean b=bean(2,14,400,300);b.setStackLevel(1);capture((KeyBoardTouchPadButton)load(1),"pad");
        controller.currentMode=KeyBoardController.ControllerMode.Active;
        event(MotionEvent.ACTION_DOWN,0,new int[]{7},550,200);
        event(MotionEvent.ACTION_POINTER_DOWN,1,new int[]{7,19},550,200,250,150);
        event(MotionEvent.ACTION_POINTER_UP,1,new int[]{7,19},550,200,250,150);
        assertEquals(List.of("pad:down","button:down","button:up"),events);
        event(MotionEvent.ACTION_UP,0,new int[]{7},550,200);assertEquals("pad:up",events.get(3));
    }
    @Test public void higherButtonsCoverDirectRegionAndRoundCornersStillPassThrough() {
        View background=new View(activity){@Override public boolean onTouchEvent(MotionEvent e){events.add("background:"+e.getActionMasked());return true;}};
        background.setId(R.id.backgroundTouchView);root.addView(background,0,new FrameLayout.LayoutParams(1000,600));
        GameMenuQuickBean area=bean(6,0,400,300);area.setStackLevel(1);load(0);
        GameMenuQuickBean b=bean(2,14,100,100);b.setStackLevel(2);b.setShapeType(0);capture((KeyBoardTouchPadButton)load(1),"round");
        controller.currentMode=KeyBoardController.ControllerMode.Active;tap(250,150);assertEquals(List.of("round:down","round:up"),events);events.clear();
        tap(202,102);assertEquals(List.of("background:0","background:1"),events);
    }
}
