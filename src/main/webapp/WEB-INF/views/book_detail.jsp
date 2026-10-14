<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${book.title} - 图书详情</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <div class="book-detail">
        <div>
            <img src="${pageContext.request.contextPath}${book.cover}" class="book-detail-cover"
                 alt="${book.title}"
                 onerror="this.src='${pageContext.request.contextPath}/images/default-book.png'">
        </div>
        <div class="book-detail-info">
            <h1>${book.title}</h1>
            <div class="meta-row"><span class="meta-label">编号：</span>${book.bookNo}</div>
            <div class="meta-row"><span class="meta-label">作者：</span>${book.author}</div>
            <div class="meta-row"><span class="meta-label">出版社：</span>${book.publisher}</div>
            <div class="meta-row">
                <span class="meta-label">出版日期：</span>
                <fmt:formatDate value="${book.publishDate}" pattern="yyyy-MM-dd"/>
            </div>
            <div class="meta-row"><span class="meta-label">ISBN：</span>${book.isbn}</div>
            <div class="meta-row">
                <span class="meta-label">分类：</span>
                <span class="tag">${book.categoryName}</span>
            </div>
            <div class="meta-row">
                <span class="meta-label">库存：</span>
                <c:choose>
                    <c:when test="${book.stock > 0}">
                        <span class="tag tag-success">${book.stock} 本可借</span>
                    </c:when>
                    <c:otherwise>
                        <span class="tag tag-danger">已借完</span>
                    </c:otherwise>
                </c:choose>
            </div>
            <div class="meta-row"><span class="meta-label">浏览次数：</span>${book.clickCount}</div>
            <div class="book-detail-price">
                ¥<fmt:formatNumber value="${book.price}" pattern="#0.00"/>
            </div>

            <c:choose>
                <c:when test="${empty sessionScope.loginUser}">
                    <a href="${pageContext.request.contextPath}/login.jsp" class="btn btn-primary btn-lg">登录后借阅</a>
                </c:when>
                <c:when test="${book.stock > 0}">
                    <form action="${pageContext.request.contextPath}/borrow/do" method="post" style="display:inline;">
                        <input type="hidden" name="bookId" value="${book.id}">
                        <button type="submit" class="btn btn-success btn-lg">📖 借阅此书</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <button class="btn btn-default btn-lg" disabled>暂无库存</button>
                </c:otherwise>
            </c:choose>

            <c:if test="${sessionScope.loginUser.role == 1}">
                <a href="${pageContext.request.contextPath}/book/edit?id=${book.id}"
                   class="btn btn-warning btn-lg">✏ 编辑</a>
                <form action="${pageContext.request.contextPath}/book/delete" method="post"
                      style="display:inline;" onsubmit="return confirm('确认删除？');">
                    <input type="hidden" name="id" value="${book.id}">
                    <button type="submit" class="btn btn-danger btn-lg">🗑 删除</button>
                </form>
            </c:if>

            <div class="book-detail-desc">
                <strong>📖 图书简介</strong><br><br>
                ${empty book.description ? '暂无简介' : book.description}
            </div>
        </div>
    </div>

    <p style="text-align:center; margin-top:20px;">
        <a href="${pageContext.request.contextPath}/book/list" class="btn btn-default">← 返回列表</a>
    </p>
</div>

</body>
</html>
