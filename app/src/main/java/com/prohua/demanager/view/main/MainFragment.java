package com.prohua.demanager.view.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.florent37.viewanimator.AnimationListener;
import com.github.florent37.viewanimator.ViewAnimator;
import com.prohua.demanager.R;
import com.prohua.demanager.adapter.DefaultAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.yokeyword.fragmentation.SupportFragment;

import static com.prohua.demanager.util.MimeUtils.getMIMEType;

/**
 * 主界面
 * Created by Deep on 2017/8/28 0028.
 */

public class MainFragment extends SupportFragment implements MainFragmentInterface {

    // 布局控件
    @BindView(R.id.recycler_header_view)
    RecyclerView recyclerViewHeader;
    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.have_not_file)
    LinearLayout haveNotFile;
    @BindView(R.id.select_view)
    LinearLayout selectBarView;

    @BindView(R.id.i_refresh)
    ImageView i_refresh;

    // 刷新动画
    private ViewAnimator refreshValueAnimator;

    // Presenter层
    private MainFragmentPresenter mainFragmentPresenter;
    // 头部View
    private DefaultAdapter headerAdapter;
    // 列表item
    private DefaultAdapter itemAdapter;

    // 再点一次退出, 程序间隔时间设置
    private static final long WAIT_TIME = 2000L;
    // 第一次触摸的时间
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
     * 初始化View控件
     */
    private void initView() {

        //设置布局管理器
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        //设置布局管理器
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(getContext());
        linearLayoutManager2.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewHeader.setLayoutManager(linearLayoutManager2);
    }

    /**
     * 初始化Presenter层
     */
    public void initData() {
        mainFragmentPresenter = new MainFragmentPresenter(this);
    }

    /**
     * 设置适配器, 必须UI线程,所以我使用了EventBus
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setDefaultAdapter(MainFragmentEvent mainFragmentEvent) {

        stopRefreshAnimator();
        // 列表适配器
        if (itemAdapter == null) {

            // 初始化适配器
            itemAdapter = new DefaultAdapter(getContext(), mainFragmentEvent.getList(), R.layout.item_home_recycler);

            // 列表的视图处理
            itemAdapter.setOnBindItemView((holder, position) -> {

                // 名称
                holder.text(R.id.f_path, mainFragmentPresenter.getPositionName(position));

                // 图标
                holder.image(R.id.img, Integer.valueOf(mainFragmentPresenter.getPositionImg(position)));

                // 选择的状态
                if (mainFragmentPresenter.getIsSelectPosition(position) == 1) {
                    holder.itemView.findViewById(R.id.i_select).setVisibility(View.VISIBLE);
                    holder.image(R.id.i_select, R.mipmap.unselect);
                } else if (mainFragmentPresenter.getIsSelectPosition(position) == 2) {
                    holder.itemView.findViewById(R.id.i_select).setVisibility(View.VISIBLE);
                    holder.image(R.id.i_select, R.mipmap.select);
                } else {
                    holder.itemView.findViewById(R.id.i_select).setVisibility(View.GONE);
                }
            });

            // 点击事件
            itemAdapter.setOnBindItemClick((view, position) -> {
                if (!mainFragmentPresenter.getIsShowSelectList()) {

                    showRefreshAnimator();

                    mainFragmentPresenter.setPathItemSelect(0);
                    mainFragmentPresenter.innerName(position);
                } else {
                    mainFragmentPresenter.setListItemSelect(position);
                    itemAdapter.notifyItemChanged(position);
                }
            });

            // 长点击事件
            itemAdapter.setOnBindItemLongClick((view, position) -> {
                if (!mainFragmentPresenter.getIsShowSelectList()) {
                    mainFragmentPresenter.setListVisibilitySelect();
                    // 显示工具栏
                    selectBarView.setVisibility(View.VISIBLE);
                    ViewAnimator.animate(selectBarView).translationY(45,0).duration(100).start();
                    // 这里我这样写有动画 当然可以用itemAdapter.notifyDataSetChanged();
                    itemAdapter.notifyItemRangeChanged(0, mainFragmentPresenter.getListSize());
                }
            });
            recyclerView.setAdapter(itemAdapter);
        } else {
            // 刷新列表
            itemAdapter.notifyDataSetChanged();
        }

        // 判断是否是空列表
        if (mainFragmentEvent.getList().size() == 0) {
            haveNotFile.setVisibility(View.VISIBLE);
        } else {
            haveNotFile.setVisibility(View.GONE);
        }

        Log.i("ftp", mainFragmentPresenter.getPathItemSelect() + "");
        // 滚动到指定位置
        if (mainFragmentPresenter.getPathItemSelect() < 2) {
            recyclerView.scrollBy(0, mainFragmentPresenter.getPathScroll());
            mainFragmentPresenter.setPathItemSelect(mainFragmentPresenter.getPathItemSelect() + 1);
        }

        // 路径适配器
        if (headerAdapter == null) {
            headerAdapter = new DefaultAdapter(getContext(), mainFragmentEvent.getPlist(), R.layout.item_home_header_recycler);

            headerAdapter.setOnBindItemView((holder, position) ->
                    holder.text(R.id.path_name, mainFragmentPresenter.getPathPosition(position))
            );
            headerAdapter.setOnBindItemClick((view, position) ->
                    mainFragmentPresenter.selectPath(position)
            );
            headerAdapter.setOnBindItemLongClick((view, position) -> {

            });

            recyclerViewHeader.setAdapter(headerAdapter);
        } else {
            // 处理数据的变化
            if (mainFragmentEvent.getPosition() == 0) { // 如果 0 则只刷新
                headerAdapter.notifyDataSetChanged();
            } else if (mainFragmentEvent.getPosition() == 1) { // 如果 1 则增加
                headerAdapter.notifyItemInserted(mainFragmentPresenter.getPathListSize());
            } else if (mainFragmentEvent.getPosition() == -1) { // 如果 -1 则减少
                headerAdapter.notifyItemRemoved(mainFragmentPresenter.getPathListSize());
            } else { // 其他就整个刷新, 不整个更换适配器出现数据不对齐, 干脆换个对象
                headerAdapter = new DefaultAdapter(getContext(), mainFragmentEvent.getPlist(), R.layout.item_home_header_recycler);

                headerAdapter.setOnBindItemView((holder, position) ->
                        holder.text(R.id.path_name, mainFragmentPresenter.getPathPosition(position))
                );
                headerAdapter.setOnBindItemClick((view, position) -> {
                    if (mainFragmentPresenter.getPathListSize() - 1 == position) {
                        mainFragmentPresenter.setPathItemSelect(2);
                    }
                    mainFragmentPresenter.selectPath(position);
                });
                headerAdapter.setOnBindItemLongClick((view, position) -> {

                });

                recyclerViewHeader.setAdapter(headerAdapter);
            }
        }

        // 滚动到最后
        recyclerViewHeader.scrollToPosition(mainFragmentPresenter.getPathListSize() - 1);

    }

    /**
     * 处理回退事件
     *
     * @return true
     */
    @Override
    public boolean onBackPressedSupport() {

        // 不是选择的状态
        if (!mainFragmentPresenter.getIsShowSelectList()) {
            if (mainFragmentPresenter.getPathListSize() == 1) {
                // LogoFragment
                if (_mActivity.getSupportFragmentManager().getBackStackEntryCount() > 2) {
                    pop();
                } else {
                    if (System.currentTimeMillis() - TOUCH_TIME < WAIT_TIME) {
                        // 杀死线程,完全退出
                        //android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
                        //System.exit(0);
                        _mActivity.finish();
                    } else {
                        TOUCH_TIME = System.currentTimeMillis();
                        // TODO: UI提示
                        Toast.makeText(getContext(), "再按一次,确定退出", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                showRefreshAnimator();
                mainFragmentPresenter.beforePath();
            }
        } else { // 取消选择
            ViewAnimator.animate(selectBarView).translationY(0,45).duration(100).onStop(() -> selectBarView.setVisibility(View.GONE)).start();

            mainFragmentPresenter.setListVisibilityUnSelect();
            itemAdapter.notifyItemRangeChanged(0, mainFragmentPresenter.getListSize());
        }
        return true;
    }

    /**
     * 滑动的距离
     *
     * @return height
     */
    public int getRecyclerViewItemScroll() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        View firstVisibItem = recyclerView.getChildAt(0);
        int firstItemPosition = layoutManager.findFirstVisibleItemPosition();
        int itemHeight = firstVisibItem.getHeight();
        int firstItemBottom = layoutManager.getDecoratedBottom(firstVisibItem);
        return (firstItemPosition + 1) * itemHeight - firstItemBottom;
    }

    /**
     * 打开文件本地打开方式
     */
    public void openFiles(String filesPath) {
        Uri uri = Uri.parse("file://" + filesPath);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);

        String type = getMIMEType(filesPath);
        intent.setDataAndType(uri, type);
        if (!type.equals("*/*")) {
            try {
                startActivity(intent);
            } catch (Exception e) {
                startActivity(showOpenTypeDialog(filesPath));
            }
        } else {
            startActivity(showOpenTypeDialog(filesPath));
        }
    }

    /**
     * 未知类型目前按txt处理方式打开, 后面采用打开dialog让用户自己选择要打开的类型的方式
     * 打开文件本地打开方式选择窗口
     */
    public static Intent showOpenTypeDialog(String param) {
        Log.e("ViChildError", "showOpenTypeDialog");
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(new File(param));
        intent.setDataAndType(uri, "text/plain");
        return intent;
    }

    /**
     * 循环显示加载动画
     */
    public void showRefreshAnimator() {
        i_refresh.setVisibility(View.VISIBLE);

        ViewAnimator
                .animate(recyclerViewHeader)
                .translationX(0, 18)
                .duration(200)
                .start();

        refreshValueAnimator = ViewAnimator
                .animate(i_refresh)
                .rotation(0, 360)
                .duration(500)
                .start();
    }

    /**
     * 取消循环显示加载动画
     */
    public void stopRefreshAnimator() {
        i_refresh.setVisibility(View.GONE);
        if (refreshValueAnimator != null) {
            refreshValueAnimator.cancel();
            ViewAnimator
                    .animate(recyclerViewHeader)
                    .translationX(18, 0)
                    .duration(200)
                    .start();
        }
    }

}
