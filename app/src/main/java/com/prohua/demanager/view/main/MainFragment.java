package com.prohua.demanager.view.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.prohua.demanager.R;
import com.prohua.demanager.adapter.DefaultAdapter;
import com.prohua.demanager.adapter.DefaultViewHolder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * 主界面
 * Created by Deep on 2017/8/28 0028.
 */

public class MainFragment extends SupportFragment implements DefaultAdapter.OnBindItemView, MainFragmentInterface {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private MainFragmentPresenter mainFragmentPresenter;

    // 再点一次退出程序时间设置
    private static final long WAIT_TIME = 2000L;
    private long TOUCH_TIME = 0;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // 注册EventBus
        EventBus.getDefault().register(this);
        // 绑定ButterKnife
        ButterKnife.bind(this, view);

        // 初始化视图
        initView();
        // 初始化数据
        initData();

        return view;
    }

    /**
     * 初始化视图
     */
    private void initView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * 初始化P层
     */
    public void initData() {
        mainFragmentPresenter = new MainFragmentPresenter(this);
    }

    /**
     * 设置适配器, 必须UI线程,所以我使用了EventBus
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setDefaultAdapter(MainFragmentEvent mainFragmentEvent) {
        DefaultAdapter defaultAdapter = new DefaultAdapter(getContext(), mainFragmentEvent.getList(), R.layout.item_home_recycler);
        defaultAdapter.setOnBindItemView(MainFragment.this);
        recyclerView.setAdapter(defaultAdapter);
    }


    @Override
    public void onBindItemViewHolder(DefaultViewHolder holder, int position) {
        holder.text(R.id.f_path, mainFragmentPresenter.getPositionName(position));
        holder.image(R.id.img, Integer.valueOf(mainFragmentPresenter.getPositionImg(position)));
    }

    /**
     * 处理回退事件
     *
     * @return true
     */
    @Override
    public boolean onBackPressedSupport() {

        // LogoFragment
        if (_mActivity.getSupportFragmentManager().getBackStackEntryCount() > 2) {
            pop();
        } else {
            if (System.currentTimeMillis() - TOUCH_TIME < WAIT_TIME) {
                // 杀死线程,完全退出
                android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
                System.exit(0);
                //_mActivity.finish();
            } else {
                TOUCH_TIME = System.currentTimeMillis();
                // TODO: UI提示
                Toast.makeText(getContext(), "再按一次,确定退出", Toast.LENGTH_LONG).show();
            }
        }

        return true;
    }
}
