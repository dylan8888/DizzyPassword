package cf.paradoxie.dizzypassword.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.ClipboardManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopeer.cardstack.CardStackView;
import com.loopeer.cardstack.StackAdapter;

import java.util.List;

import cf.paradoxie.dizzypassword.AppManager;
import cf.paradoxie.dizzypassword.MyApplication;
import cf.paradoxie.dizzypassword.R;
import cf.paradoxie.dizzypassword.activity.AddActivity;
import cf.paradoxie.dizzypassword.db.AccountBean;
import cf.paradoxie.dizzypassword.db.RxBean;
import cf.paradoxie.dizzypassword.utils.DesUtil;
import cf.paradoxie.dizzypassword.utils.RxBus;
import cf.paradoxie.dizzypassword.utils.SPUtils;
import cf.paradoxie.dizzypassword.view.DialogView;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.UpdateListener;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class TestStackAdapter extends StackAdapter<Integer> {
    private static List<AccountBean> mBeanList;

    public TestStackAdapter(Context context, List list) {
        super(context);
        this.mBeanList = list;
    }

    @Override
    public void bindView(Integer data, int position, CardStackView.ViewHolder holder) {
        if (holder instanceof ColorItemViewHolder) {
            ColorItemViewHolder h = (ColorItemViewHolder) holder;
            h.onBind(data, position);
        }
    }

    @Override
    protected CardStackView.ViewHolder onCreateView(ViewGroup parent, int viewType) {
        View view;
        view = getLayoutInflater().inflate(R.layout.list_card_item, parent, false);
        return new ColorItemViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.list_card_item;
    }

    static class ColorItemViewHolder extends CardStackView.ViewHolder {
        View mLayout;
        View mContainerContent;
        TextView mTextTitle, mNum, mTime, mTime_up, mTag1, mTag2, mTag3, mTag4, mTag5, mAccount, mPassword, mPwdvisible, mNote;
        Button mChange, mDelete;
        RxBean rxEvent, rxEvent_1;
        ImageView iv_copy;
        private SweetAlertDialog pDialog = null;
        private static Boolean isSure = false;//删除确认
        private static Boolean isShow = false;//密码默认false不显示

        public ColorItemViewHolder(View view) {
            super(view);
            mLayout = view.findViewById(R.id.frame_list_card_item);
            mContainerContent = view.findViewById(R.id.container_list_content);
            mTextTitle = (TextView) view.findViewById(R.id.text_list_card_title);
            mNum = (TextView) view.findViewById(R.id.text_list_card_num);
            mTime = (TextView) view.findViewById(R.id.text_list_card_time);
            mTime_up = (TextView) view.findViewById(R.id.text_list_card_up);
            mAccount = (TextView) view.findViewById(R.id.tv_account);
            mPassword = (TextView) view.findViewById(R.id.tv_password);
            mPwdvisible = (TextView) view.findViewById(R.id.tv_password_visible);
            mNote = (TextView) view.findViewById(R.id.tv_note);
            mChange = (Button) view.findViewById(R.id.bt_change);
            mDelete = (Button) view.findViewById(R.id.bt_delete);
            iv_copy = (ImageView) view.findViewById(R.id.iv_copy);


            mTag1 = (TextView) view.findViewById(R.id.text_list_card_tag1);
            mTag2 = (TextView) view.findViewById(R.id.text_list_card_tag2);
            mTag3 = (TextView) view.findViewById(R.id.text_list_card_tag3);
            mTag4 = (TextView) view.findViewById(R.id.text_list_card_tag4);
            mTag5 = (TextView) view.findViewById(R.id.text_list_card_tag5);
            rxEvent = new RxBean();
            rxEvent_1 = new RxBean();

            mTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent_1.setAction("done");
                    RxBus.getInstance().post(rxEvent_1);
                }
            });
            mTime_up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent_1.setAction("done");
                    RxBus.getInstance().post(rxEvent_1);
                }
            });

            mTag1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent.setMessage(mTag1.getText().toString().trim());
                    RxBus.getInstance().post(rxEvent);
                }
            });
            mTag2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent.setMessage(mTag2.getText().toString().trim());
                    RxBus.getInstance().post(rxEvent);
                }
            });
            mTag3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent.setMessage(mTag3.getText().toString().trim());
                    RxBus.getInstance().post(rxEvent);
                }
            });
            mTag4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent.setMessage(mTag4.getText().toString().trim());
                    RxBus.getInstance().post(rxEvent);
                }
            });
            mTag5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    rxEvent.setMessage(mTag5.getText().toString().trim());
                    RxBus.getInstance().post(rxEvent);
                }
            });

        }

        @Override
        public void onItemExpand(boolean b) {
            mContainerContent.setVisibility(b ? View.VISIBLE : View.GONE);
        }

        public void onBind(Integer data, final int position) {
            final String id = mBeanList.get(position).getObjectId();
            final String time = mBeanList.get(position).getCreatedAt();
            final String time_up = mBeanList.get(position).getUpdatedAt();
            final String name = DesUtil.decrypt(mBeanList.get(position).getName().toString(), SPUtils.getKey());
            final String account = DesUtil.decrypt(mBeanList.get(position).getAccount().toString(), SPUtils.getKey());
            final String password = DesUtil.decrypt(mBeanList.get(position).getPassword().toString(), SPUtils.getKey());
            String note = mBeanList.get(position).getNote().toString();
            if (note != null) {
                note = DesUtil.decrypt(mBeanList.get(position).getNote().toString(), SPUtils.getKey());
            }
            final List<String> tag = mBeanList.get(position).getTag();

            mLayout.getBackground().setColorFilter(ContextCompat.getColor(getContext(), data), PorterDuff.Mode.SRC_IN);
            mTextTitle.setText(name);
            mNum.setText(String.valueOf(position + 1) + "-" + mBeanList.size());
            mTime.setText(time + "  创建");
            mTime_up.setText(time_up + "  更新");
            mAccount.setText(account);
            //            mPassword.setText(password);
            mPassword.setText("**********");
            mNote.setText(note);
            mPwdvisible.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isShow == false) {
                        if (MyApplication.first_check == 0) {
                            DialogView dialogView = new DialogView(AppManager.getAppManager().currentActivity());//这操作可以
                            dialogView.setAccount(SPUtils.get("name", "") + "");
                            if (!AppManager.getAppManager().currentActivity().isFinishing()) {
                                dialogView.show();
                            }
                            dialogView.setOnPosNegClickListener(new DialogView.OnPosNegClickListener() {
                                @Override
                                public void posClickListener(String value) {
                                    hideInputWindow();
                                    //校验密码
                                    if (value.equals(SPUtils.get("password", "") + "")) {
                                        mPassword.setText(password);
                                        setDrawableLeft(R.drawable.password_open);
                                        isShow = true;
                                        iv_copy.setVisibility(View.VISIBLE);
                                        MyApplication.first_check++;
                                    } else {
                                        MyApplication.showToast("密码错了哦~");
                                    }
                                }

                                @Override
                                public void negCliclListener(String value) {
                                    //取消查看
                                    hideInputWindow();
                                }
                            });

                        } else {
                            mPassword.setText(password);
                            setDrawableLeft(R.drawable.password_open);
                            isShow = true;
                            iv_copy.setVisibility(View.VISIBLE);
                        }
                    } else {
                        setDrawableLeft(R.drawable.password);
                        mPassword.setText("**********");
                        isShow = false;
                        iv_copy.setVisibility(View.GONE);
                    }
                }
            });

            mAccount.setOnLongClickListener(new View.OnLongClickListener() {
                @SuppressLint("ResourceAsColor")
                @Override
                public boolean onLongClick(View view) {
                    ClipboardManager cm = (ClipboardManager) MyApplication.mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(mAccount.getText());
                    MyApplication.showSnack(view, R.string.copy_account);
                    return false;
                }
            });
            mPassword.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClipboardManager cm = (ClipboardManager) MyApplication.mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(mPassword.getText());
                    MyApplication.showSnack(view, R.string.copy_password);
                    return false;
                }
            });
            mNote.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ClipboardManager cm = (ClipboardManager) MyApplication.mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(mNote.getText());
                    MyApplication.showSnack(view, R.string.copy_note);
                    return false;
                }
            });

            final String finalNote = note;
            mChange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (MyApplication.first_check == 0) {
                        DialogView dialogView = new DialogView(AppManager.getAppManager().currentActivity());
                        dialogView.setAccount(SPUtils.get("name", "") + "");
                        if (!AppManager.getAppManager().currentActivity().isFinishing()) {
                            dialogView.show();
                        }
                        dialogView.setOnPosNegClickListener(new DialogView.OnPosNegClickListener() {
                            @Override
                            public void posClickListener(String value) {
                                //校验密码
                                if (value.equals(SPUtils.get("password", "") + "")) {
                                   changeDate(name,account,password,finalNote,tag,id);
                                    MyApplication.first_check++;
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
                        changeDate(name,account,password,finalNote,tag,id);
                    }
                }
            });

            mDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (MyApplication.first_check == 0) {
                        DialogView dialogView = new DialogView(AppManager.getAppManager().currentActivity());
                        dialogView.setAccount(SPUtils.get("name", "") + "");
                        if (!AppManager.getAppManager().currentActivity().isFinishing()) {
                            dialogView.show();
                        }
                        dialogView.setOnPosNegClickListener(new DialogView.OnPosNegClickListener() {
                            @Override
                            public void posClickListener(String value) {
                                //校验密码
                                if (value.equals(SPUtils.get("password", "") + "")) {
                                    showDelete(id,account,password);
                                    MyApplication.first_check++;
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
                        showDelete(id,account,password);
                    }
                }
            });

            iv_copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mAccount.getText().length() > 0 || mPassword.getText().length() > 0) {
                        ClipboardManager cm = (ClipboardManager) MyApplication.mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setText(mAccount.getText() + "\n" + mPassword.getText());
                        MyApplication.showSnack(view, R.string.copy);
                    } else {
                        MyApplication.showSnack(view, R.string.nothing_copy);
                    }
                }
            });

            if (tag.size() == 0) {
                return;
            }
            if (tag.size() == 1) {
                mTag1.setText(tag.get(0));
                mTag1.setVisibility(View.VISIBLE);
            }
            if (tag.size() == 2) {
                mTag1.setText(tag.get(0));
                mTag2.setText(tag.get(1));
                mTag1.setVisibility(View.VISIBLE);
                mTag2.setVisibility(View.VISIBLE);
            }
            if (tag.size() == 3) {
                mTag1.setText(tag.get(0));
                mTag2.setText(tag.get(1));
                mTag3.setText(tag.get(2));

                showTag(true);
                mTag4.setVisibility(View.GONE);
                mTag5.setVisibility(View.GONE);
            }
            if (tag.size() == 4) {
                mTag1.setText(tag.get(0));
                mTag2.setText(tag.get(1));
                mTag3.setText(tag.get(2));
                mTag4.setText(tag.get(3));

                showTag(true);
                mTag5.setVisibility(View.GONE);
            }
            if (tag.size() == 5) {
                mTag1.setText(tag.get(0));
                mTag2.setText(tag.get(1));
                mTag3.setText(tag.get(2));
                mTag4.setText(tag.get(3));
                mTag5.setText(tag.get(4));
                showTag(true);
            }

        }

        private void changeDate(String name, String account, String password, String finalNote, List<String> tag,String  id){
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("name", name);
            bundle.putString("account", account);
            bundle.putString("password", password);
            bundle.putString("finalNote", finalNote);
            bundle.putString("tag", DesUtil.listToString(tag, " "));
            bundle.putString("id", id);

            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//在非activity中调用intent必须设置，不然部分手机崩溃
            MyApplication.getContext().startActivity(intent.setClass(MyApplication.getContext(), AddActivity.class));
            AppManager.getAppManager().currentActivity().finish();
        }

        private void showDelete(final String id, String account, String password) {

            pDialog = new SweetAlertDialog(AppManager.getAppManager().currentActivity(), SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            pDialog.setTitleText("Loading");
            pDialog.setCancelable(false);
            new SweetAlertDialog(AppManager.getAppManager().currentActivity(), SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("删除此条帐号信息")
                    .setContentText(
                            "帐号:" + account + "\n密码:" + password +
                                    "\n\n您确定要删除么😟")
                    .setConfirmText("确定")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            deleteDate(id);
                            sDialog.cancel();
                        }
                    })
                    .setCancelText("不删了")
                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.cancel();
                        }
                    })
                    .show();

        }

        private void deleteDate(String id) {
            pDialog.show();
            //删除当前数据
            AccountBean accountBean = new AccountBean();
            accountBean.setObjectId(id);
            accountBean.delete(new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if (e == null) {
                        mTextTitle.setText("已删除");
                        mAccount.setText("已删除");
                        mPassword.setText("已删除");
                        showTag(false);
                        mTime_up.setVisibility(View.GONE);
                        mTime.setVisibility(View.GONE);
                        mNote.setText("本条帐号信息删除成功，请点击右上角刷新按钮");
                        mDelete.setText("删除成功");
                        mDelete.setClickable(false);
                        iv_copy.setVisibility(View.GONE);
                    } else {
                        if (e.getErrorCode() == 101) {
                            MyApplication.showToast("已经删掉了哦~");
                        } else {
                            MyApplication.showToast("删除失败：" + e.getMessage() + "," + e.getErrorCode());
                        }
                    }
                    pDialog.dismiss();
                }
            });
        }

        /**
         * 隐藏软键盘
         */
        private void hideInputWindow() {
            InputMethodManager imm = (InputMethodManager) MyApplication.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(AppManager.getAppManager().currentActivity().getWindow().getDecorView().getWindowToken(), 0);
        }

        /**
         * 设置密码左边的图片显示
         *
         * @param id
         */
        private void setDrawableLeft(int id) {
            Drawable drawableLeft = MyApplication.getContext().getResources().getDrawable(id);
            drawableLeft.setBounds(0, 0, drawableLeft.getMinimumWidth(), drawableLeft.getMinimumHeight());
            mPwdvisible.setCompoundDrawables(drawableLeft, null, null, null);
        }

        private void showTag(Boolean b) {
            if (b) {
                mTag1.setVisibility(View.VISIBLE);
                mTag2.setVisibility(View.VISIBLE);
                mTag3.setVisibility(View.VISIBLE);
                mTag4.setVisibility(View.VISIBLE);
                mTag5.setVisibility(View.VISIBLE);
            } else {
                mTag1.setVisibility(View.GONE);
                mTag2.setVisibility(View.GONE);
                mTag3.setVisibility(View.GONE);
                mTag4.setVisibility(View.GONE);
                mTag5.setVisibility(View.GONE);
            }
        }
    }

}
