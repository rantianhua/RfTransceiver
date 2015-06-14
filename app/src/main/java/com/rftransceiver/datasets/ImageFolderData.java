package com.rftransceiver.datasets;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by rantianhua on 15-6-11.
 * this class used to save every image folder's information
 */
public class ImageFolderData {

    /**
     * the name of folder
     */
    private String name;

    /**
     * the first image path in the folder
     */
    private String firstPicPath;

    /**
     * the absolutely path of the folder
     */
    private String abPath;

    /**
     * the counts of images in this folder
     */
    private int counts;

    /**
     * all images paths in this folder
     */
    private List<String> paths;

    public static final String CAMERA = "//camera";

    private boolean selected = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbPath() {
        return abPath;
    }

    public void setAbPath(String abPath) {
        this.abPath = abPath;
        //get folder name in absolutely path
        int index = abPath.lastIndexOf("/");
        setName(abPath.substring(index+1));
    }

    public int getCounts() {
        return counts;
    }

    public void setCounts(int counts) {
        this.counts = counts;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(String[] paths) {
        String[] datas = new String[paths.length+1];
        System.arraycopy(paths,0,datas,0,paths.length);
        paths = null;
        datas[datas.length-1] = CAMERA;
        this.paths = Arrays.asList(datas);
        Collections.reverse(this.paths);
        setCounts(this.paths.size()-1);
        setFirstPicPath(getAbPath()+"/"+this.paths.get(1));
    }

    public String getFirstPicPath() {
        return firstPicPath;
    }

    public void setFirstPicPath(String firstPicPath) {
        this.firstPicPath = firstPicPath;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
