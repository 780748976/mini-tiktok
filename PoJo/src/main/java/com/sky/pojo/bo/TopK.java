package com.sky.pojo.bo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class TopK {

    private int k = 0;

    private Queue<HotVideo> queue;

    public TopK(int k, Queue<HotVideo> queue){
        this.k = k;
        this.queue = queue;
    }

    public void add(HotVideo hotVideo) {
        if (queue.size() < k) {
            queue.add(hotVideo);
        } else if (queue.peek().getHot() < hotVideo.getHot()){
            queue.poll();
            queue.add(hotVideo);
        }
    }


    public List<HotVideo> get(){
        final ArrayList<HotVideo> list = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            list.add(queue.poll());
        }
        Collections.reverse(list);
        return list;
    }

}
