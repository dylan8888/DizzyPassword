package cf.paradoxie.dizzypassword.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.loopeer.cardstack.CardStackView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cf.paradoxie.dizzypassword.AppManager;
import cf.paradoxie.dizzypassword.MyApplication;
import cf.paradoxie.dizzypassword.R;
import cf.paradoxie.dizzypassword.adapter.TestStackAdapter;
import cf.paradoxie.dizzypassword.db.AccountBean;
import cf.paradoxie.dizzypassword.db.RxBean;
import cf.paradoxie.dizzypassword.utils.DesUtil;
import cf.paradoxie.dizzypassword.utils.MyToast;
import cf.paradoxie.dizzypassword.utils.RxBus;
import cf.paradoxie.dizzypassword.utils.SPUtils;
import cf.paradoxie.dizzypassword.utils.SortByTime;
import cf.paradoxie.dizzypassword.utils.ThemeUtils;
import cf.paradoxie.dizzypassword.view.DialogView;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobPointer;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.pedant.SweetAlert.SweetAlertDialog;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity implements CardStackView.ItemExpendListener, View.OnClickListener {
    private boolean optionMenuOn = true;  //显示optionmenu
    private Menu aMenu;         //获取optionmenu
    public static Integer[] TEST_DATAS = new Integer[]{
            R.color.color_1, R.color.color_2, R.color.color_3, R.color.color_4
            , R.color.color_5, R.color.color_6
            , R.color.color_7, R.color.color_8,
            R.color.color_9, R.color.color_10, R.color.color_11, R.color.color_12,
            R.color.color_13, R.color.color_14, R.color.color_15, R.color.color_16,
            R.color.color_17, R.color.color_18, R.color.color_19, R.color.color_20,
            R.color.color_21, R.color.color_22, R.color.color_23, R.color.color_24,
            R.color.color_25, R.color.color_26
    };

    private CardStackView mStackView;
    private TestStackAdapter mTestStackAdapter;

    private List<AccountBean> mAccountBeans;
    private TextView tip;
    private SweetAlertDialog pDialog = null;
    private static Boolean isExit = false;
    private BmobUser user = new BmobUser();
    public static Toolbar toolbar = null;
    private DialogView mDialogView;
    private LinearLayout main_btn;
    private long mCurrentPlayTime;
    private ObjectAnimator animator;
    private ImageView refresh, red_package, setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ThemeUtils.initStatusBarColor(MainActivity.this, ThemeUtils.getPrimaryDarkColor(MainActivity.this));

        refresh = (ImageView) findViewById(R.id.refresh);
        red_package = (ImageView) findViewById(R.id.red_package);
        setting = (ImageView) findViewById(R.id.setting);
        refresh.setOnClickListener(this);
        red_package.setOnClickListener(this);
        setting.setOnClickListener(this);
        main_btn = (LinearLayout) findViewById(R.id.main_btn);
        main_btn.setVisibility(View.VISIBLE);
        //检测menu操作，第二次进入app时是否显示menu
        if (!(Boolean) SPUtils.get("optionMenuOn", true)) {
            optionMenuOn = false;
            checkOptionMenu();
        }
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (SPUtils.get("name", "") != "") {
            if (MyApplication.first_check == 0) {
                toolbar.setNavigationIcon(R.drawable.yep_selector);
            } else {
                toolbar.setNavigationIcon(R.drawable.yep);
            }
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MyApplication.first_check == 0) {
                        checkActivity();
                        mDialogView.setOnPosNegClickListener(new DialogView.OnPosNegClickListener() {
                            @Override
                            public void posClickListener(String value) {
                                //校验密码
                                if (value.equals(SPUtils.get("password", "") + "")) {
                                    MyApplication.first_check++;
                                    hideInputWindow();
                                    //换图标，解锁
                                    toolbar.setNavigationIcon(R.drawable.yep);
                                    mDialogView.dismiss();
                                } else {
                                    MyApplication.showToast("密码错了哦~");
                                }
                            }

                            @Override
                            public void negCliclListener(String value) {
                                //取消查看
                            }
                        });
                    } else {
                        new SweetAlertDialog(MainActivity.this, SweetAlertDialog.WARNING_TYPE)
                                .setTitleText("锁定数据操作")
                                .setContentText(
                                        "帐号:" + (String.valueOf(SPUtils.get("name", ""))) + "\n密码:" + (String.valueOf(SPUtils.get("password", ""))) +
                                                "\n\n确定要锁定当前操作权限么？")
                                .setConfirmText("锁定")
                                .setCancelText("算啦")
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        MyApplication.first_check = 0;

                                        //换图标：加锁
                                        toolbar.setNavigationIcon(R.drawable.yep_selector);
                                        sDialog.cancel();
                                    }
                                })
                                .show();
                    }
                }
            });
        }

        tip = (TextView) findViewById(R.id.tip);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (MyApplication.isSign()) {
                    if (MyApplication.first_check == 0) {
                        checkActivity();
                        mDialogView.setOnPosNegClickListener(new DialogView.OnPosNegClickListener() {
                            @Override
                            public void posClickListener(String value) {
                                //校验密码
                                if (value.equals(SPUtils.get("password", "") + "")) {
                                    Intent intent = new Intent(MainActivity.this, AddActivity.class);
                                    startActivity(intent);
                                    MyApplication.first_check++;
                                    hideInputWindow();
                                    mDialogView.dismiss();
                                    toolbar.setNavigationIcon(R.drawable.yep);
                                } else {
                                    MyApplication.showToast("密码错了哦~");
                                }
                            }

                            @Override
                            public void negCliclListener(String value) {
                                //取消查看
                            }
                        });
                    } else {
                        Intent intent = new Intent(MainActivity.this, AddActivity.class);
                        startActivity(intent);
                        //                        finish();
                    }
                } else {
                    //缓存用户对象为空时， 可打开用户注册界面…
                    Intent intent = new Intent(MainActivity.this, SignActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        mStackView = (CardStackView) findViewById(R.id.stackview_main);
        mStackView.setItemExpendListener(this);

        if (SPUtils.get("key", "") + "" == "") {
                        Bmob.initialize(this, "46b1709520ec4d0afa17e505680202da");//正式版
//            Bmob.initialize(this, "949a1379183be6d8a655037d1282c146");//测试版
        } else {
            Bmob.initialize(this, SPUtils.get("key", "") + "");
        }
        if (!MyApplication.isSign()) {
            tip.setVisibility(View.VISIBLE);
        } else {
            //取缓存数据
            if (SPUtils.getDataList("beans", AccountBean.class).size() < 1) {
                findOnLineDate();
            } else {
                findOffLineDate();
            }

        }

        final SortByTime sortByTime = new SortByTime();
        RxBus.getInstance().toObserverable(RxBean.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<RxBean>() {
                    @Override
                    public void call(RxBean rxBean) {
                        if (rxBean.getMessage() != null) {
                            searchDate(rxBean.getMessage());
                            return;
                        }
                        if (rxBean.getAction() != null) {
                            if (mStackView.isExpending()) {
                                mStackView.clearSelectPosition();
                                mStackView.removeAllViews();
                            }
                            findDateByTime(sortByTime);
                            return;
                        }
                    }

                });
    }


    private void findOffLineDate() {

        pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("离线数据加载中...");
        pDialog.show();
        mAccountBeans = SPUtils.getDataList("beans", AccountBean.class);

        mTestStackAdapter = new TestStackAdapter(MainActivity.this, mAccountBeans);
        mStackView.setAdapter(mTestStackAdapter);
        mTestStackAdapter.notifyDataSetChanged();
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        //为什么不能把TEST_DATA拿出来单独处理一次，会出现ANR
                        mTestStackAdapter.updateData(Arrays.asList(DesUtil.getRandomFromArray(TEST_DATAS, mAccountBeans.size())));
                        pDialog.dismiss();
                    }
                }
                , 1500
        );
    }

    private void checkActivity() {
        mDialogView = new DialogView(MainActivity.this);
        mDialogView.setAccount(SPUtils.get("name", "") + "");
        try {
            if (!MainActivity.this.isFinishing()) {
                mDialogView.show();
                hideInputWindow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchDate(String s) {
        //手动清除一次全部view，避免重用时的重合
        if (mStackView.isExpending()) {
            mStackView.clearSelectPosition();
            mStackView.removeAllViews();
        }
        pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("查找中...");
        if (!MainActivity.this.isFinishing()) {
            pDialog.show();
        }
        BmobQuery<AccountBean> query = new BmobQuery<AccountBean>();
        String[] search = {s};
        query.addWhereContainsAll("tag", Arrays.asList(search));
        query.addWhereEqualTo("user", new BmobPointer(user));
        query.findObjects(new FindListener<AccountBean>() {

            @Override
            public void done(List<AccountBean> object, BmobException e) {
                if (object.size() != 0) {
                    mAccountBeans = object;
                    mTestStackAdapter = new TestStackAdapter(MainActivity.this, mAccountBeans);
                    mStackView.setAdapter(mTestStackAdapter);
                    mTestStackAdapter.notifyDataSetChanged();
                    new Handler().postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    //为什么不能把TEST_DATA拿出来单独处理一次，会出现ANR

                                    mTestStackAdapter.updateData(Arrays.asList(DesUtil.getRandomFromArray(TEST_DATAS, mAccountBeans.size())));
                                }
                            }
                            , 100
                    );

                } else {
                    MyApplication.showToast("不知道哪里出问题了" + e);
                }
                pDialog.dismiss();
            }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SPUtils.get("key", "") + "" != "") {
            Bmob.initialize(this, SPUtils.get("key", "") + "");
        }
    }

    private void findOnLineDate() {
        pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setTitleText("联网加载中...");
        pDialog.show();
        startAnim();
        BmobQuery<AccountBean> query = new BmobQuery<>();
        if (MyApplication.getUser() != null) {
            String id = MyApplication.getUser().getObjectId();
            user.setObjectId(id);
            query.addWhereEqualTo("user", new BmobPointer(user));
            query.findObjects(new FindListener<AccountBean>() {

                @Override
                public void done(List<AccountBean> objects, BmobException e) {
                    if (objects != null) {
                        mAccountBeans = objects;
                        if (mAccountBeans.size() < 1) {
                            if (SPUtils.getDataList("beans", AccountBean.class).size() < 1) {
                                tip.setText("好像还没有记录什么帐号信息，点击右下角添加吧(*^__^*)");
                                tip.setVisibility(View.VISIBLE);
                                pDialog.dismiss();
                                stopAnim(animator);
                                return;
                            }
                        }
                        tip.setVisibility(View.GONE);
                        //缓存
                        SPUtils.setDataList("beans", mAccountBeans);
                        mTestStackAdapter = new TestStackAdapter(MainActivity.this, mAccountBeans);
                        mStackView.setAdapter(mTestStackAdapter);
                        mTestStackAdapter.notifyDataSetChanged();
                        new Handler().postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        //为什么不能把TEST_DATA拿出来单独处理一次，会出现ANR
                                        mTestStackAdapter.updateData(Arrays.asList(DesUtil.getRandomFromArray(TEST_DATAS, mAccountBeans.size())));
                                        // hideAnimate();
                                        stopAnim(animator);
                                    }
                                }
                                , 100
                        );
                    } else {
                        if (e.getErrorCode() == 9016 && SPUtils.getDataList("beans", AccountBean.class).size() > 1) {
                            MyApplication.showToast("网络好像不可以哦~");
                            stopAnim(animator);
                            pDialog.dismiss();
                            return;
                        } else {
                            tip.setText("好像还没有记录什么帐号信息，点击右下角添加吧(*^__^*)");
                            tip.setVisibility(View.VISIBLE);
                        }
                    }
                    pDialog.dismiss();

                }
            });
        }

    }

    private void findDateByTime(final SortByTime sortByTime) {
        try {//233，这个地方单独判断isFinish还是会崩，得再捕捉一次
            if (!MainActivity.this.isFinishing()) {
                pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.PROGRESS_TYPE);
                pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                pDialog.setTitleText("加载中");
                pDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        BmobQuery<AccountBean> query = new BmobQuery<>();
        if (MyApplication.getUser() != null) {
            String id = MyApplication.getUser().getObjectId();
            user.setObjectId(id);
            query.addWhereEqualTo("user", new BmobPointer(user));
            query.findObjects(new FindListener<AccountBean>() {

                @Override
                public void done(List<AccountBean> objects, BmobException e) {
                    if (objects != null) {
                        Collections.sort(objects, Collections.reverseOrder(sortByTime));
                        mAccountBeans = objects;
                        MyToast.show(MainActivity.this, "已按最近更新时间排序", ThemeUtils.getPrimaryColor(AppManager.getAppManager().currentActivity()));

                        if (mAccountBeans.size() < 1) {
                            tip.setText("好像还没有记录什么帐号信息，点击右下角添加吧(*^__^*)");
                            tip.setVisibility(View.VISIBLE);
                            pDialog.dismiss();
                            return;
                        }
                        tip.setVisibility(View.GONE);
                        mTestStackAdapter = new TestStackAdapter(MainActivity.this, mAccountBeans);
                        mStackView.setAdapter(mTestStackAdapter);
                        mTestStackAdapter.notifyDataSetChanged();
                        new Handler().postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        //为什么不能把TEST_DATA拿出来单独处理一次，会出现ANR
                                        mTestStackAdapter.updateData(Arrays.asList(DesUtil.getRandomFromArray(TEST_DATAS, mAccountBeans.size())));
                                    }
                                }
                                , 100
                        );
                    } else {
                        tip.setText("好像还没有记录什么帐号信息，点击右下角添加吧(*^__^*)");
                        tip.setVisibility(View.VISIBLE);
                    }
                    if (pDialog != null && pDialog.isShowing()) {
                        pDialog.dismiss();
                    }

                }
            });
        }


    }


    //是否显示menu
    private void checkOptionMenu() {
        if (null != aMenu) {
            if (optionMenuOn) {
                for (int i = 0; i < aMenu.size(); i++) {
                    aMenu.getItem(i).setVisible(true);
                    aMenu.getItem(i).setEnabled(true);
                }
            } else {
                for (int i = 0; i < aMenu.size(); i++) {
                    aMenu.getItem(i).setVisible(false);
                    aMenu.getItem(i).setEnabled(false);
                }
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        aMenu = menu;
        checkOptionMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onItemExpend(boolean expend) {

    }

    @Override
    public void onBackPressed() {
        exitBy2Click();
    }

    private void exitBy2Click() {
        Timer tExit = null;
        if (!isExit) {
            isExit = true;
            // 准备退出
            MyApplication.showToast("再按一次退出程序");
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            finish();
            System.exit(0);
        }
    }

    /**
     * 隐藏软键盘
     */
    private void hideInputWindow() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                if (MyApplication.isSign()) {
                    if (mStackView.isExpending()) {
                        mStackView.clearSelectPosition();
                        mStackView.removeAllViews();
                    }
                    findOnLineDate();

                } else {
                    MyApplication.showToast("您还木有登录哦~");

                }
                break;
            case R.id.red_package:
                new SweetAlertDialog(MainActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("口令复制成功")
                        .setContentText("支付宝红包，金额随机，最高￥99喔😃\n" +
                                "\n每天都可以来领取一次哈\n话说最近的红包好像都变大了呢...\n")
                        .setConfirmText("前往支付宝领取")
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                ClipboardManager cm = (ClipboardManager) MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                cm.setText(getString(R.string.red_package_string_little));
                                try {
                                    MyApplication.openAppByPackageName(MainActivity.this, "com.eg.android.AlipayGphone");
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                sDialog.cancel();
                            }
                        })
                        .show();
                break;
            case R.id.setting:
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    private void startAnim() {
        animator = ObjectAnimator.ofFloat(refresh, "rotation", 0f, 360.0f);
        animator.setDuration(500);
        animator.setInterpolator(new LinearInterpolator());//不停顿
        animator.setRepeatCount(-1);//设置动画重复次数
        animator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
        startAnimation(animator);
    }

    private void stopAnim(ObjectAnimator mRotateAntiClockwiseAnimator) {
        mCurrentPlayTime = mRotateAntiClockwiseAnimator.getCurrentPlayTime();
        mRotateAntiClockwiseAnimator.cancel();
    }

    private void startAnimation(ObjectAnimator mRotateAntiClockwiseAnimator) {
        mRotateAntiClockwiseAnimator.start();
        mRotateAntiClockwiseAnimator.setCurrentPlayTime(mCurrentPlayTime);
    }
}
