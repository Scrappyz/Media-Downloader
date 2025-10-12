package com.scrappyz.ytdlp.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ThreadUtils {
    
    public static Thread getThreadByName(String threadName) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            if (thread.getName().equals(threadName)) {
                return thread;
            }
        }
        return null; // Thread not found
    }

    public static List<String> getActiveThreadsByName() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        List<String> threadList = new ArrayList<>();
        for(Thread thread : threadSet) {
            threadList.add(thread.getName());
        }

        return threadList;
    }
    
}
