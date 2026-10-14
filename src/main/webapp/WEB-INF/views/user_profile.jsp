<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>个人中心 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <div class="card">
        <div class="card-title">👤 个人中心</div>

        <div style="display:flex; align-items:center; gap:24px; margin-bottom:24px;">
            <img src="${pageContext.request.contextPath}${user.avatar}" class="avatar avatar-lg"
                 onerror="this.src='${pageContext.request.contextPath}/images/default-avatar.png'">
            <div>
                <h2>${user.username}
                    <c:if test="${user.role == 1}">
                        <span class="badge-admin">管理员</span>
                    </c:if>
                </h2>
                <p style="color:#909399; margin-top:6px;">
                    注册时间：<fmt:formatDate value="${user.createTime}" pattern="yyyy-MM-dd"/>
                </p>
            </div>
        </div>

        <form action="${pageContext.request.contextPath}/user/update" method="post"
              enctype="multipart/form-data">

            <div class="form-row">
                <div class="form-group">
                    <label>用户名</label>
                    <input type="text" class="form-control" value="${user.username}" disabled>
                </div>
                <div class="form-group">
                    <label>真实姓名</label>
                    <input type="text" name="realName" class="form-control" value="${user.realName}">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>邮箱</label>
                    <input type="email" name="email" class="form-control" value="${user.email}">
                </div>
                <div class="form-group">
                    <label>手机号</label>
                    <input type="text" name="phone" class="form-control" value="${user.phone}"
                           pattern="1[3-9]\d{9}">
                </div>
            </div>

            <div class="form-group">
                <label>性别</label>
                <label style="font-weight:normal; margin-right:16px;">
                    <input type="radio" name="gender" value="男" ${user.gender == '男' ? 'checked' : ''}> 男
                </label>
                <label style="font-weight:normal;">
                    <input type="radio" name="gender" value="女" ${user.gender == '女' ? 'checked' : ''}> 女
                </label>
            </div>

            <div class="form-group">
                <label>所在地区</label>
                <div class="region-row">
                    <select name="province" id="province" class="form-control">
                        <option value="">请选择省份</option>
                    </select>
                    <select name="city" id="city" class="form-control">
                        <option value="">请选择城市</option>
                    </select>
                    <select name="district" id="district" class="form-control">
                        <option value="">请选择区/县</option>
                    </select>
                </div>
                <div class="form-tip">
                    当前：${user.province} ${user.city} ${user.district}
                </div>
            </div>

            <div class="form-group">
                <label>详细地址</label>
                <input type="text" name="address" class="form-control" value="${user.address}">
            </div>

            <div class="form-group">
                <label>头像</label>
                <input type="file" name="avatarFile" accept="image/*" class="form-control">
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-primary">💾 保存修改</button>
            </div>
        </form>
    </div>
</div>

<script>
    const ctx = '${pageContext.request.contextPath}';
    const currentProvince = '${user.province}';
    const currentCity = '${user.city}';
    const currentDistrict = '${user.district}';

    function loadRegion(level, parent, selectEl, placeholder, selected, next) {
        selectEl.innerHTML = '<option value="">' + placeholder + '</option>';
        let url = ctx + '/region?level=' + level;
        if (parent) url += '&parent=' + encodeURIComponent(parent);
        const xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.onload = function() {
            if (xhr.status === 200) {
                try {
                    const list = JSON.parse(xhr.responseText);
                    list.forEach(function(item) {
                        const opt = document.createElement('option');
                        opt.value = item;
                        opt.textContent = item;
                        if (item === selected) opt.selected = true;
                        selectEl.appendChild(opt);
                    });
                    if (next) next();
                } catch (e) {}
            }
        };
        xhr.send();
    }

    const pEl = document.getElementById('province');
    const cEl = document.getElementById('city');
    const dEl = document.getElementById('district');

    loadRegion('province', null, pEl, '请选择省份', currentProvince, function() {
        if (currentProvince) {
            loadRegion('city', currentProvince, cEl, '请选择城市', currentCity, function() {
                if (currentCity) {
                    loadRegion('district', currentCity, dEl, '请选择区/县', currentDistrict, null);
                }
            });
        }
    });

    pEl.addEventListener('change', function() {
        cEl.innerHTML = '<option value="">请选择城市</option>';
        dEl.innerHTML = '<option value="">请选择区/县</option>';
        if (this.value) loadRegion('city', this.value, cEl, '请选择城市', null, null);
    });
    cEl.addEventListener('change', function() {
        dEl.innerHTML = '<option value="">请选择区/县</option>';
        if (this.value) loadRegion('district', this.value, dEl, '请选择区/县', null, null);
    });
</script>

</body>
</html>
