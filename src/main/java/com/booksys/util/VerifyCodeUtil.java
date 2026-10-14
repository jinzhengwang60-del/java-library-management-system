package com.booksys.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

/**
 * 图片验证码生成工具
 */
public class VerifyCodeUtil {

    private static final int WIDTH = 110;
    private static final int HEIGHT = 36;
    private static final int CODE_LEN = 4;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final Random random = new Random();

    /**
     * 生成验证码字符串
     */
    public static String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 把验证码写到输出流（PNG格式）
     */
    public static void writeImage(String code, OutputStream out) throws IOException {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(120, 200));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        // 字符
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(20, 130));
            int x = 18 + i * 22;
            int y = 25 + random.nextInt(5);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        // 噪点
        for (int i = 0; i < 50; i++) {
            g.setColor(randomColor(50, 200));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }

        g.dispose();
        ImageIO.write(img, "png", out);
    }

    private static Color randomColor(int min, int max) {
        int r = min + random.nextInt(max - min);
        int gColor = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return new Color(r, gColor, b);
    }
}
