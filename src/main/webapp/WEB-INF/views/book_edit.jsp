<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>${empty book.id ? '新增图书' : '编辑图书'} - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <div class="card">
        <div class="card-title">
            ${empty book.id ? '➕ 新增图书' : '✏ 编辑图书'}
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error">${fn:escapeXml(error)}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/book/save" method="post"
              enctype="multipart/form-data">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <c:if test="${not empty book.id}">
                <input type="hidden" name="id" value="${book.id}">
                <input type="hidden" name="version" value="${book.version}">
            </c:if>

            <div class="form-row">
                <div class="form-group">
                    <label>图书编号 <span style="color:red">*</span></label>
                    <input type="text" name="bookNo" class="form-control" required
                           value="${fn:escapeXml(book.bookNo)}" placeholder="如 B016">
                </div>
                <div class="form-group">
                    <label>书名 <span style="color:red">*</span></label>
                    <input type="text" name="title" class="form-control" required value="${fn:escapeXml(book.title)}">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>作者 <span style="color:red">*</span></label>
                    <input type="text" name="author" class="form-control" required value="${fn:escapeXml(book.author)}">
                </div>
                <div class="form-group">
                    <label>出版社</label>
                    <input type="text" name="publisher" class="form-control" value="${fn:escapeXml(book.publisher)}">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>出版日期</label>
                    <input type="date" name="publishDate" class="form-control"
                           value="<fmt:formatDate value='${book.publishDate}' pattern='yyyy-MM-dd'/>">
                </div>
                <div class="form-group">
                    <label>ISBN</label>
                    <input type="text" name="isbn" class="form-control" value="${fn:escapeXml(book.isbn)}">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>分类 <span style="color:red">*</span></label>
                    <select name="categoryId" class="form-control" required>
                        <option value="">请选择分类</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat.id}" ${book.categoryId == cat.id ? 'selected' : ''}>${fn:escapeXml(cat.name)}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group">
                    <label>价格 <span style="color:red">*</span></label>
                    <input type="number" name="price" step="0.01" min="0" class="form-control"
                           required value="${book.price}">
                </div>
                <div class="form-group">
                    <label>可借库存数量 <span style="color:red">*</span></label>
                    <input type="number" name="stock" min="0" class="form-control"
                           required value="${empty book.stock ? 0 : book.stock}">
                </div>
            </div>

            <div class="form-group">
                <label>封面图片（不上传则使用默认图）</label>
                <input type="file" name="coverFile" accept="image/*" class="form-control">
                <c:if test="${not empty book.cover}">
                    <div class="form-tip">
                        当前封面：<img src="${pageContext.request.contextPath}${fn:escapeXml(book.cover)}"
                                       style="height:80px; vertical-align:middle; margin-left:8px;">
                    </div>
                </c:if>
            </div>

            <div class="form-group">
                <label>图书简介</label>
                <textarea name="description" class="form-control" rows="4">${fn:escapeXml(book.description)}</textarea>
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-primary">💾 保存</button>
                <a href="${pageContext.request.contextPath}/book/list" class="btn btn-default">取消</a>
            </div>
        </form>
    </div>
</div>

</body>
</html>
