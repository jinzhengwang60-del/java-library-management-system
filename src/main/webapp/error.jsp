<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>出错了 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-box" style="text-align:center;">
        <h1 style="font-size:80px; color:#f56c6c; margin-bottom:20px;">⚠</h1>
        <h2 style="color:#2c3e50; margin-bottom:12px;">页面出错了</h2>
        <p style="color:#909399; margin-bottom:24px;">
            抱歉，您访问的页面发生了一些问题。
        </p>
        <% if (exception != null) { %>
            <div class="alert alert-error" style="text-align:left;">
                <%= exception.getMessage() %>
            </div>
        <% } %>
        <a href="<%=request.getContextPath()%>/book/list" class="btn btn-primary">返回首页</a>
    </div>
</div>
</body>
</html>
