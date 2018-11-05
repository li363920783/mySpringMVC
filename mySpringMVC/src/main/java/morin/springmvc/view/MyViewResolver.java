package morin.springmvc.view;

/**
 * MyViewResolver class
 * 视图解析器
 * @author Molrin
 * @date 2018/11/5 0005
 */
public class MyViewResolver {

    /**
     * 视图前缀
     */
    private String prefix;
    /**
     * 视图后缀
     */
    private String suffix;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String jspMapping(String value) {
        return prefix+value+suffix;
    }
}
