package com.booksys.servlet;

import com.booksys.util.ImageStore;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.nio.file.*;

@WebServlet("/upload/*")
public class UploadServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getPathInfo();
        if (name == null || !name.matches("/[0-9a-f]{32}\\.png")) { response.sendError(404); return; }
        Path file = ImageStore.directory().resolve(name.substring(1));
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) { response.sendError(404); return; }
        response.setContentType("image/png");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=86400, immutable");
        response.setContentLengthLong(Files.size(file));
        Files.copy(file, response.getOutputStream());
    }
}
