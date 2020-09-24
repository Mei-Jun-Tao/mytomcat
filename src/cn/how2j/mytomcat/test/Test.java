package cn.how2j.mytomcat.test;

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.Map;

public class Test {

    private Map<String, String> css = new HashMap<>();

    public Test(){

    }

    public static void main(String[] args) {

        Test t1 = new Test();
        System.out.println("1:" + t1.css);
        t1.css.put("haha", "hello");
        System.out.println("2:" + t1.css);
        Test t2 = new Test();
        System.out.println("3:" + t2.css);
        System.out.println(t2.css.get("haha"));
    }
}


