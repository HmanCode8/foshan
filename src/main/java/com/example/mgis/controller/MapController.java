package com.example.mgis.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/map")
public class MapController {

    @Value("${map.tk}")
    private String tk;

    // 天地图多域名 t0~t7，轮询使用，避免单域名限流{insert\_element\_0\_}
    private static final List<String> TDTS = Arrays.asList("t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7");
    private static final AtomicInteger INDEX = new AtomicInteger(0);

    // 单例 RestTemplate
    private static final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/tiles")
    public void getTiandituTile(
            HttpServletRequest request,
            @RequestParam("layer") String layer,
            @RequestParam("TileMatrix") int z,
            @RequestParam("TileCol") int x,
            @RequestParam("TileRow") int y,
            HttpServletResponse response
    ) throws IOException {
        // 关闭重试，加速
        System.setProperty("sun.net.http.retryPost", "false");

        // 轮询取一个域名
        String host = TDTS.get(INDEX.getAndIncrement() % TDTS.size());

        // 拼接URL（HTTPS、layer_c、参数齐全）{insert\_element\_1\_}
        String url = "https://" + host + ".tianditu.gov.cn/" + layer + "_c/wmts"
                + "?layer=" + layer
                + "&style=default&tilematrixset=c&Service=WMTS&Request=GetTile&Version=1.0.0&Format=image/png"
                + "&TileMatrix=" + z + "&TileCol=" + x + "&TileRow=" + y
                + "&tk=" + tk;

        try {
            byte[] body = restTemplate.getForObject(url, byte[].class);
            if (body != null) {
                response.setContentType("image/png");
                // 缓存1天，减轻服务器压力
                response.setHeader("Cache-Control", "max-age=86400");
                response.getOutputStream().write(body);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            // 可加日志：e.printStackTrace();
            System.out.println("天地图e = " + e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}