package com.rftransceiver.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by rantianhua on 15-6-27.
 */
public class PoolThreadUtil {

    /**
     * the single instance
     */
    private static PoolThreadUtil poolThreadUtil;

    private ExecutorService executorService;

    private PoolThreadUtil() {
        executorService = Executors.newFixedThreadPool(30);
    }

    /**
     * get the single instance of PoolThreadUtil
     * @return
     */
    public static PoolThreadUtil getInstance() {
        if(poolThreadUtil == null) {
            poolThreadUtil = new PoolThreadUtil();
        }
        return poolThreadUtil;
    }

    /**
     * add new task implemented Runnable to run
     * @param task
     */
    public synchronized void addTask(Runnable task) {
        this.executorService.execute(task);
    }

    /**
     * close the executorServices
     */
    public void  close () {
        if(executorService == null || poolThreadUtil == null) return;
        executorService.shutdown();
        try {
            if(!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            executorService.shutdownNow();
        }finally {
            executorService = null;
            poolThreadUtil = null;
        }
    }
}
