<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">
<title>请求未完成 - 图书管理系统</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css"></head>
<body><div class="auth-page"><div class="auth-box" style="text-align:center">
<h2>请求未完成</h2><div class="alert alert-error">
<c:choose>
<c:when test="${requestScope['javax.servlet.error.status_code'] >= 500}">服务暂时不可用，请稍后重试。</c:when>
<c:otherwise>${fn:escapeXml(requestScope['javax.servlet.error.message'])}</c:otherwise>
</c:choose></div>
<a class="btn btn-primary" href="${pageContext.request.contextPath}/book/list">返回图书列表</a>
</div></div></body></html>
