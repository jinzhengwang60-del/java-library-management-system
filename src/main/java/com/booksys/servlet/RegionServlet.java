package com.booksys.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * 三级行政区联动接口（Ajax）
 * 任务书要求：用户注册地址的分级生成
 *
 * GET /region?level=province
 * GET /region?level=city&parent=河南省
 * GET /region?level=district&parent=郑州市
 *
 * 这里使用静态数据，覆盖几个常见省份（足够课程设计演示）
 */
@WebServlet("/region")
public class RegionServlet extends HttpServlet {

    private static final Map<String, List<String>> PROVINCE_CITY = new LinkedHashMap<>();
    private static final Map<String, List<String>> CITY_DISTRICT = new LinkedHashMap<>();

    static {
        // 省 -> 市
        PROVINCE_CITY.put("北京市", Arrays.asList("北京市"));
        PROVINCE_CITY.put("上海市", Arrays.asList("上海市"));
        PROVINCE_CITY.put("河南省", Arrays.asList("郑州市", "洛阳市", "开封市", "新乡市"));
        PROVINCE_CITY.put("江苏省", Arrays.asList("南京市", "苏州市", "无锡市", "常州市", "南通市"));
        PROVINCE_CITY.put("浙江省", Arrays.asList("杭州市", "宁波市", "温州市", "嘉兴市", "绍兴市"));
        PROVINCE_CITY.put("广东省", Arrays.asList("广州市", "深圳市", "东莞市", "佛山市"));

        // 市 -> 区
        CITY_DISTRICT.put("北京市", Arrays.asList("东城区", "西城区", "海淀区", "朝阳区", "丰台区"));
        CITY_DISTRICT.put("上海市", Arrays.asList("黄浦区", "徐汇区", "长宁区", "静安区", "浦东新区"));
        CITY_DISTRICT.put("郑州市", Arrays.asList("中原区", "二七区", "金水区", "高新区", "管城区"));
        CITY_DISTRICT.put("洛阳市", Arrays.asList("涧西区", "西工区", "老城区", "洛龙区"));
        CITY_DISTRICT.put("开封市", Arrays.asList("鼓楼区", "龙亭区", "顺河区"));
        CITY_DISTRICT.put("新乡市", Arrays.asList("红旗区", "卫滨区", "凤泉区"));
        CITY_DISTRICT.put("南京市", Arrays.asList("玄武区", "鼓楼区", "建邺区", "秦淮区", "雨花台区"));
        CITY_DISTRICT.put("苏州市", Arrays.asList("姑苏区", "工业园区", "高新区", "吴中区", "相城区"));
        CITY_DISTRICT.put("无锡市", Arrays.asList("梁溪区", "锡山区", "惠山区", "滨湖区"));
        CITY_DISTRICT.put("常州市", Arrays.asList("天宁区", "钟楼区", "新北区", "武进区"));
        CITY_DISTRICT.put("南通市", Arrays.asList("崇川区", "港闸区", "通州区"));
        CITY_DISTRICT.put("杭州市", Arrays.asList("上城区", "下城区", "西湖区", "滨江区", "余杭区"));
        CITY_DISTRICT.put("宁波市", Arrays.asList("海曙区", "江北区", "镇海区", "北仑区", "鄞州区"));
        CITY_DISTRICT.put("温州市", Arrays.asList("鹿城区", "龙湾区", "瓯海区"));
        CITY_DISTRICT.put("嘉兴市", Arrays.asList("南湖区", "秀洲区"));
        CITY_DISTRICT.put("绍兴市", Arrays.asList("越城区", "柯桥区", "上虞区"));
        CITY_DISTRICT.put("广州市", Arrays.asList("天河区", "越秀区", "海珠区", "白云区", "番禺区"));
        CITY_DISTRICT.put("深圳市", Arrays.asList("福田区", "罗湖区", "南山区", "宝安区", "龙岗区"));
        CITY_DISTRICT.put("东莞市", Arrays.asList("莞城街道", "东城街道", "南城街道"));
        CITY_DISTRICT.put("佛山市", Arrays.asList("禅城区", "南海区", "顺德区"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String level = request.getParameter("level");
        String parent = request.getParameter("parent");
        PrintWriter out = response.getWriter();

        List<String> list = null;
        if ("province".equals(level)) {
            list = new ArrayList<>(PROVINCE_CITY.keySet());
        } else if ("city".equals(level)) {
            list = PROVINCE_CITY.get(parent);
        } else if ("district".equals(level)) {
            list = CITY_DISTRICT.get(parent);
        }

        if (list == null) list = Collections.emptyList();

        // 输出 JSON 数组
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        out.write(sb.toString());
    }
}
