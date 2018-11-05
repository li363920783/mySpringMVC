package morin.springmvc.controller;

import morin.springmvc.annotation.MyController;
import morin.springmvc.annotation.MyRequestMapping;

/**
 * TestController class
 *
 * @author Molrin
 * @date 2018/11/5 0005
 */
@MyController
@MyRequestMapping("/testController")
public class TestController {

    @MyRequestMapping("/test")
    public String testHello(){
        System.out.println("执行controller");
        System.out.println("老子成功啦!");
        return "index";
    }


}
