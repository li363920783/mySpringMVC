package morin.springmvc.servlet;

import morin.springmvc.annotation.MyController;
import morin.springmvc.annotation.MyRequestMapping;
import morin.springmvc.view.MyViewResolver;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * MyDispathcerServlet class
 *
 * @author Molrin
 * @date 2018/11/5 0005
 */
public class MyDispathcerServlet extends HttpServlet {
    /**
     * 模拟IDC容器,保存bean对象
     */
    private Map<String, Object> iocContainer = new HashMap<String, Object>();
    /**
     * 保存handler映射
     */
    private Map<String, Method> methods = new HashMap<String, Method>();
    /**
     * 视图解析器
     */
    private MyViewResolver myViewResolver;

    @Override
    public void init(ServletConfig config) throws ServletException {
        //扫描Controller,创建实例对象,并存入iocContainer
        scanController(config);
        //初始化Handler映射
        initHandlerMapping();
        //加载视图解析器
        loadViewResolver(config);
    }

    /**
     * 加载视图解析器
     * @param config 配置
     */
    private void loadViewResolver(ServletConfig config) {
        //创建SAXReader解析xml配置
        SAXReader saxReader = new SAXReader();
        try {
            //解析springMVC.xml
            String path = config.getServletContext().getRealPath("") + "\\WEB-INF\\classes\\" + config.getInitParameter("contextConfigLocation");
            Document document = saxReader.read(path);
            Element root = document.getRootElement();
            Iterator iterator = root.elementIterator();
            while (iterator.hasNext()) {
                Element element = (Element) iterator.next();
                if ("bean".equals(element.getName())) {
                    String className = element.attributeValue("class");
                    Class<?> aClass = Class.forName(className);
                    Object o = aClass.newInstance();
                    //获取方法对象
                    Method setPrefix = aClass.getMethod("setPrefix", String.class);
                    Method setSuffix = aClass.getMethod("setSuffix", String.class);
                    Iterator beanIter = element.elementIterator();
                    //获取property值
                    HashMap<String, String> map = new HashMap<String, String>();
                    while (beanIter.hasNext()) {
                        Element next = (Element) beanIter.next();
                        String name = next.attributeValue("name");
                        String value = next.attributeValue("value");
                        map.put(name, value);
                    }
                    for (String key : map.keySet()) {
                        //反射机制调用set方法,完成赋值
                        if ("prefix".equals(key)) {
                            setPrefix.invoke(o, map.get(key));
                        }
                        if ("suffix".equals(key)) {
                            setSuffix.invoke(o, map.get(key));
                        }
                    }
                    myViewResolver = (MyViewResolver)o;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化handler映射
     */
    private void initHandlerMapping() {
        Set<String> keySet = iocContainer.keySet();
        for (String value : keySet) {
            //获取每个类的class对象
            Class<?> aClass = iocContainer.get(value).getClass();
            //获取该类的方法数组
            Method[] methods = aClass.getMethods();
            for (Method method : methods) {
                //判断方法是否带有MyRequestMapping注解
                if (method.isAnnotationPresent(MyRequestMapping.class)) {
                    //获取该注解中的值
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String ann = annotation.value().substring(1);
                    //将带有注解的方法存入处理器映射器
                    this.methods.put(ann, method);
                }
            }
        }
    }

    /**
     * 扫描controller
     *
     * @param config 配置
     */
    private void scanController(ServletConfig config) {
        //创建解析xml对象
        SAXReader saxReader = new SAXReader();
        try {
            //获取xml路径
            String path = config.getServletContext().getRealPath("") + "\\WEB-INF\\classes\\" + config.getInitParameter("contextConfigLocation");
            //获取document
            Document document = saxReader.read(path);
            //获取根元素
            Element rootElement = document.getRootElement();
            //获取元素迭代器
            Iterator iterator = rootElement.elementIterator();
            //循环遍历
            while (iterator.hasNext()) {
                //获取单个元素
                Element element = (Element) iterator.next();
                //判断是否有包扫描标签
                if ("component-scan".equals(element.getName())) {
                    //获取扫描的包名
                    String basePackage = element.attributeValue("base-package");
                    //获取basePackage包下的所有类名
                    List<String> classNames = getClassName(basePackage);
                    for (String className : classNames) {
                        //根据全限定名获取class对象
                        Class<?> aClass = Class.forName(className);
                        //判断是否有@Controller注解
                        if (aClass.isAnnotationPresent(MyController.class)) {
                            MyRequestMapping requestMapping = aClass.getAnnotation(MyRequestMapping.class);
                            //获取该注解中的value
                            String value = requestMapping.value().substring(1);
                            //使用instance方法创建Controller对象并放入iocContainer
                            iocContainer.put(value, aClass.newInstance());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> getClassName(String basePackage) {
        //创建返回值
        List<String> classNames = new ArrayList<String>();
        //将全限定名中的. 全部替换为/
        String newNames = basePackage.replace(".", "/");
        //获取当前线程的类加载器
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL url = contextClassLoader.getResource(newNames);
        if (url != null) {
            File file = new File(url.getPath());
            //获取该目录下的所有文件路径的File对象集合
            getFileName(file.listFiles(), basePackage,classNames);
           /* File[] childFile = file.listFiles();
            if (childFile != null) {
                for (File file1 : childFile) {
                    //获取每个文件的全限定路径名,去掉后缀名
                    File[] files = file1.listFiles();
                    if (files != null) {
                        for (File file2 : files) {
                            String className = basePackage +"."+ file2.getName().replace(".class", "");
                            classNames.add(className);
                        }
                    }

                }
            }*/
        }
        return classNames;
    }
    private void getFileName(File[] file,String basePackage,List<String> classNames){
        String path;
        if (file != null) {
            for (File file1 : file) {
                if (!file1.isFile()) {
                    File[] files = file1.listFiles();
                    path = basePackage+"." +file1.getName();
                    getFileName(files,path,classNames);
                }else {
                    path = basePackage + "." + file1.getName().replace(".class", "");
                    classNames.add(path);
                }
            }
        }
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取请求
        String handlerUri = req.getRequestURI().split("/")[1];
        //获取controller实例
        Object obj = iocContainer.get(handlerUri);
        String methodUri = req.getRequestURI().split("/")[2];
        //获取method实例
        Method method = methods.get(methodUri);
        try {
            //反射机制调用方法
            String value = (String) method.invoke(obj);
            //视图解析器将逻辑视图转换成物理视图
            String view = myViewResolver.jspMapping(value);
            //页面跳转
            req.getRequestDispatcher(view).forward(req, resp);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
