<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>图书列表 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <!-- 搜索/筛选栏 -->
    <form action="${pageContext.request.contextPath}/book/list" method="get" class="filter-bar">
        <input type="text" name="keyword" value="${fn:escapeXml(keyword)}" placeholder="搜索书名、作者或ISBN...">
        <select name="categoryId">
            <option value="">所有分类</option>
            <c:forEach var="cat" items="${categories}">
                <option value="${cat.id}" ${selectedCategoryId == cat.id ? 'selected' : ''}>${fn:escapeXml(cat.name)}</option>
            </c:forEach>
        </select>
        <button type="submit" class="btn btn-primary">🔍 搜索</button>
        <a href="${pageContext.request.contextPath}/book/list" class="btn btn-default">重置</a>
    </form>

    <!-- 分类快速导航 -->
    <div class="category-nav">
        <a href="${pageContext.request.contextPath}/book/list"
           class="${empty selectedCategoryId ? 'active' : ''}">全部</a>
        <c:forEach var="cat" items="${categories}">
            <a href="${pageContext.request.contextPath}/book/list?categoryId=${cat.id}"
               class="${selectedCategoryId == cat.id ? 'active' : ''}">${fn:escapeXml(cat.name)}</a>
        </c:forEach>
    </div>

    <div class="main-layout">
        <!-- 主内容：图书网格 -->
        <div>
            <c:choose>
                <c:when test="${empty pageBean.list}">
                    <div class="card" style="text-align:center; padding:60px;">
                        <p style="color:#909399; font-size:16px;">😢 没有找到相关图书</p>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="book-grid">
                        <c:forEach var="book" items="${pageBean.list}">
                            <a href="${pageContext.request.contextPath}/book/detail?id=${book.id}"
                               style="color:inherit;">
                                <div class="book-card">
                                    <img src="${pageContext.request.contextPath}${fn:escapeXml(book.cover)}"
                                         class="book-card-img" alt="${fn:escapeXml(book.title)}"
                                         onerror="this.src='${pageContext.request.contextPath}/images/default-book.png'">
                                    <div class="book-card-body">
                                        <div class="book-card-title">${fn:escapeXml(book.title)}</div>
                                        <div class="book-card-meta">作者：${fn:escapeXml(book.author)}</div>
                                        <div class="book-card-meta">
                                            <c:if test="${not empty book.categoryName}">
                                                <span class="tag">${fn:escapeXml(book.categoryName)}</span>
                                            </c:if>
                                        </div>
                                        <div class="book-card-price">
                                            ¥<fmt:formatNumber value="${book.price}" pattern="#0.00"/>
                                        </div>
                                        <div class="book-card-foot">
                                            <span>库存: ${book.stock}</span>
                                            <span>👁 ${book.clickCount}</span>
                                        </div>
                                    </div>
                                </div>
                            </a>
                        </c:forEach>
                    </div>

                    <!-- 分页 -->
                    <c:set var="paginationPath" value="/book/list" scope="request"/><jsp:include page="_pagination.jsp"/>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- 侧边栏 -->
        <div>
            <!-- 点击量排行榜 -->
            <div class="sidebar-card">
                <div class="sidebar-title">🔥 热门图书 TOP 5</div>
                <ul class="sidebar-list">
                    <c:forEach var="top" items="${topBooks}" varStatus="st">
                        <li>
                            <span>
                                <span class="rank">${st.count}</span>
                                <a href="${pageContext.request.contextPath}/book/detail?id=${top.id}">${fn:escapeXml(top.title)}</a>
                            </span>
                            <span class="count">${top.clickCount}</span>
                        </li>
                    </c:forEach>
                </ul>
            </div>

            <!-- 在线用户 -->
            <div class="sidebar-card online-users-card" style="margin-top:20px;">
                <div class="sidebar-title">🟢 在线用户</div>
                <c:set var="onlineUsers" value="${applicationScope.onlineUsers}"/>
                <c:choose>
                    <c:when test="${empty onlineUsers}">
                        <p style="color:#909399; font-size:13px;">暂无在线用户</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="sidebar-list">
                            <c:forEach var="u" items="${onlineUsers}">
                                <li>
                                    <span><span class="online-dot"></span>${fn:escapeXml(u.username)}</span>
                                    <c:if test="${u.role == 1}">
                                        <span class="badge-admin">管理员</span>
                                    </c:if>
                                </li>
                            </c:forEach>
                        </ul>
                        <p style="margin-top:8px; color:#909399; font-size:12px;">
                            当前共 ${fn:length(onlineUsers)} 人在线
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

</body>
</html>
