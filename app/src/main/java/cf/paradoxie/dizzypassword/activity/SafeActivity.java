package cf.paradoxie.dizzypassword.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import cf.paradoxie.dizzypassword.MyApplication;
import cf.paradoxie.dizzypassword.R;
import cf.paradoxie.dizzypassword.utils.SPUtils;
import cf.paradoxie.dizzypassword.view.FingerPrinterView;
import cn.pedant.SweetAlert.SweetAlertDialog;
import rx.Subscriber;
import rx.Subscription;
import zwh.com.lib.FPerException;
import zwh.com.lib.RxFingerPrinter;

import static zwh.com.lib.CodeException.FINGERPRINTERS_FAILED_ERROR;
import static zwh.com.lib.CodeException.HARDWARE_MISSIING_ERROR;
import static zwh.com.lib.CodeException.KEYGUARDSECURE_MISSIING_ERROR;
import static zwh.com.lib.CodeException.NO_FINGERPRINTERS_ENROOLED_ERROR;
import static zwh.com.lib.CodeException.PERMISSION_DENIED_ERROE;

/**
 * Created by xiehehe on 2017/10/30.
 */

public class SafeActivity extends AppCompatActivity {

    private FingerPrinterView fingerPrinterView;
    private int fingerErrorNum = 0; // 指纹错误次数
    RxFingerPrinter rxfingerPrinter;
    private RelativeLayout rl_support_finger, rl_unsupport_finger;
    private EditText et_pwd;
    private Button bt_pwd;
    private TextView tv_message;
    int code = 999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe);
        rl_support_finger = (RelativeLayout) findViewById(R.id.rl_support_finger);
        rl_unsupport_finger = (RelativeLayout) findViewById(R.id.rl_unsupport_finger);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {//sdk23以下的版本直接数字码验证
            codeCheck();
            tv_message.setText("当前设备版本过低，请使用6位数字码进行安全验证");
        } else {//SDK23以上显示指纹
            fingerCheck();
            if (code != 999) {
                codeCheck();
                if (code == PERMISSION_DENIED_ERROE) {
                    tv_message.setText("当前没有指纹权限，将使用6位数字码进行安全验证或在设置中授权后重试");
                } else if (code == HARDWARE_MISSIING_ERROR) {
                    tv_message.setText("当前没有指纹模块，将使用6位数字码进行安全验证");
                } else if (code == KEYGUARDSECURE_MISSIING_ERROR) {
                    tv_message.setText("当前没有设置锁屏密码，将使用6位数字码进行安全验证");
                } else if (code == NO_FINGERPRINTERS_ENROOLED_ERROR) {
                    tv_message.setText("当前没有指纹录入，将使用6位数字码进行安全验证或在设置中录入指纹后重试");
                } else if (code == FINGERPRINTERS_FAILED_ERROR) {
                    tv_message.setText("指纹认证失败，将使用6位数字码进行安全验证");
                }

            } else {
                rl_support_finger.setVisibility(View.VISIBLE);
            }

        }
    }

    /*
    指纹验证
     */
    private void fingerCheck() {
        fingerPrinterView = (FingerPrinterView) findViewById(R.id.fpv);
        fingerPrinterView.setOnStateChangedListener(new FingerPrinterView.OnStateChangedListener() {
            @Override
            public void onChange(int state) {
                if (state == FingerPrinterView.STATE_CORRECT_PWD) {
                    fingerErrorNum = 0;
                    Toast.makeText(MyApplication.getContext(), "指纹验证成功", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MyApplication.getContext(), MainActivity.class));
                    finish();
                }
                if (state == FingerPrinterView.STATE_WRONG_PWD) {
                    Toast.makeText(MyApplication.getContext(), "指纹验证失败，还剩" + (5 - fingerErrorNum) + "次机会",
                            Toast.LENGTH_SHORT).show();
                    fingerPrinterView.setState(FingerPrinterView.STATE_NO_SCANING);
                }
            }
        });
        rxfingerPrinter = new RxFingerPrinter(this);
        startFinger();


    }

    /*
    数字码验证
     */
    private void codeCheck() {
        final String str = SPUtils.get("pwd", "") + "";
        rl_unsupport_finger.setVisibility(View.VISIBLE);
        et_pwd = (EditText) findViewById(R.id.et_pwd);
        bt_pwd = (Button) findViewById(R.id.bt_pwd);
        tv_message = (TextView) findViewById(R.id.tv_message);
        bt_pwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String pwd = et_pwd.getText().toString().trim();
                if (str.equals("")) {//还没有设置安全码

                    new SweetAlertDialog(SafeActivity.this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("安全码设置")
                            .setContentText("您正在进行首次安全码设置，以后进入APP都将使用此安全码进行验证，您的安全码为\n" + pwd)
                            .setCancelText("我再想想")
                            .setConfirmText("确定")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    SPUtils.put("pwd", pwd);
                                    sDialog.dismissWithAnimation();
                                    startActivity(new Intent(MyApplication.getContext(), MainActivity.class));
                                    finish();
                                }
                            })
                            .showCancelButton(true)
                            .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    sDialog.cancel();
                                }
                            })
                            .show();
                } else {//设置了安全码，开始比对
                    if (pwd.equals(str)) {
                        MyApplication.showToast("验证成功");
                        startActivity(new Intent(MyApplication.getContext(), MainActivity.class));
                        finish();
                    } else {
                        MyApplication.showToast("安全码验证错误，请重新尝试");
                    }
                }
            }
        });
    }

    private void startFinger() {
        fingerErrorNum = 0;
        rxfingerPrinter.unSubscribe(this);
        Subscription subscription = rxfingerPrinter.begin().subscribe(new Subscriber<Boolean>() {
            @Override
            public void onStart() {
                super.onStart();
                if (fingerPrinterView.getState() == FingerPrinterView.STATE_SCANING) {
                    return;
                } else if (fingerPrinterView.getState() == FingerPrinterView.STATE_CORRECT_PWD
                        || fingerPrinterView.getState() == FingerPrinterView.STATE_WRONG_PWD) {
                    fingerPrinterView.setState(FingerPrinterView.STATE_NO_SCANING);
                } else {
                    fingerPrinterView.setState(FingerPrinterView.STATE_SCANING);
                }
            }

            @Override
            public void onCompleted() {

            }

            @SuppressLint("WrongConstant")
            @Override
            public void onError(Throwable e) {
                if (e instanceof FPerException) {
                    code = ((FPerException) e).getCode();
                }
            }

            @Override
            public void onNext(Boolean aBoolean) {
                if (aBoolean) {
                    fingerPrinterView.setState(FingerPrinterView.STATE_CORRECT_PWD);
                } else {
                    fingerErrorNum++;
                    fingerPrinterView.setState(FingerPrinterView.STATE_WRONG_PWD);
                }
            }
        });
        rxfingerPrinter.addSubscription(this, subscription);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rxfingerPrinter != null) {
            rxfingerPrinter.unSubscribe(this);
        }
    }
}
