package com.prohua.demanager.view.main;

import com.prohua.demanager.R;
import com.prohua.demanager.util.GetFilesUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Deep on 2017/8/29 0029.
 */

public class MainFragmentPresenter {

    private MainFragmentModel mainFragmentModel;

    private MainFragmentInterface mainFragmentInterface;

    public MainFragmentPresenter(MainFragmentInterface view) {
        mainFragmentInterface = view;
        mainFragmentModel = new MainFragmentModel();
    }

    public void loadFolderList() throws IOException {
        mainFragmentModel.setBaseFile(GetFilesUtils.getInstance().getBasePath());
        loadFolderList(mainFragmentModel.getBaseFile());
    }

    public void loadFolderList(String file) throws IOException {
        List<Map<String, Object>> list = GetFilesUtils.getInstance().getSonNode(file);
        if(list!=null){
            Collections.sort(list, GetFilesUtils.getInstance().defaultOrder());
            mainFragmentModel.getList().clear();
            for(Map<String, Object> map:list){
                String fileType=(String) map.get(GetFilesUtils.FILE_INFO_TYPE);
                Map<String,Object> gMap=new HashMap<String, Object>();
                if(map.get(GetFilesUtils.FILE_INFO_ISFOLDER).equals(true)){
                    gMap.put("fIsDir", true);
                    gMap.put("fImg", R.mipmap.dir );
                    gMap.put("fInfo", map.get(GetFilesUtils.FILE_INFO_NUM_SONDIRS)+"个文件夹和"+
                            map.get(GetFilesUtils.FILE_INFO_NUM_SONFILES)+"个文件");
                }else{
                    gMap.put("fIsDir", false);
                    if(fileType.equals("txt")||fileType.equals("text")){
                        gMap.put("fImg", R.mipmap.file);
                    }else{
                        gMap.put("fImg", R.mipmap.file);
                    }
                    gMap.put("fInfo","文件大小:"+GetFilesUtils.getInstance().getFileSize(map.get(GetFilesUtils.FILE_INFO_PATH).toString()));
                }
                gMap.put("fName", map.get(GetFilesUtils.FILE_INFO_NAME));
                gMap.put("fPath", map.get(GetFilesUtils.FILE_INFO_PATH));
                mainFragmentModel.getList().add(gMap);
            }
        }else{
            mainFragmentModel.getList().clear();
        }

        MainFragmentEvent event = new MainFragmentEvent();
        event.setList(mainFragmentModel.getList());
        EventBus.getDefault().post(event);
    }

    public String getPositionName(int position) {
        return mainFragmentModel.getList().get(position).get("fName").toString();
    }

    public String getPositionImg(int position) {
        return mainFragmentModel.getList().get(position).get("fImg").toString();
    }
}