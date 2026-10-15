<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>注册 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-box wide">
        <div class="auth-title">
            <h2>📝 用户注册</h2>
            <p>创建您的图书管理账号</p>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error">${fn:escapeXml(error)}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/register" method="post" id="registerForm">
            <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
            <div class="form-row">
                <div class="form-group">
                    <label>用户名 <span style="color:red">*</span></label>
                    <input type="text" name="username" id="username" class="form-control"
                           required minlength="3" maxlength="20" placeholder="3-20位字符">
                    <div class="form-tip" id="usernameTip"></div>
                </div>
                <div class="form-group">
                    <label>真实姓名</label>
                    <input type="text" name="realName" class="form-control" maxlength="20">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>密码 <span style="color:red">*</span></label>
                    <input type="password" name="password" id="password" class="form-control"
                           required minlength="6" placeholder="至少6位">
                    <div class="form-tip" id="passwordTip"></div>
                </div>
                <div class="form-group">
                    <label>确认密码 <span style="color:red">*</span></label>
                    <input type="password" id="password2" class="form-control"
                           required placeholder="再次输入密码">
                    <div class="form-tip" id="password2Tip"></div>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label>邮箱 <span style="color:red">*</span></label>
                    <input type="email" name="email" id="email" class="form-control" required>
                    <div class="form-tip" id="emailTip"></div>
                </div>
                <div class="form-group">
                    <label>手机号 <span style="color:red">*</span></label>
                    <input type="text" name="phone" id="phone" class="form-control"
                           required pattern="1[3-9]\d{9}" placeholder="11位手机号">
                    <div class="form-tip" id="phoneTip"></div>
                </div>
            </div>

            <div class="form-group">
                <label>性别</label>
                <label style="font-weight:normal; margin-right:16px;">
                    <input type="radio" name="gender" value="男" checked> 男
                </label>
                <label style="font-weight:normal;">
                    <input type="radio" name="gender" value="女"> 女
                </label>
            </div>

            <div class="form-group">
                <label>所在地区（三级联动）</label>
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
            </div>

            <div class="form-group">
                <label>详细地址</label>
                <input type="text" name="address" class="form-control" placeholder="街道、门牌号等">
            </div>

            <button type="submit" class="btn btn-primary btn-block btn-lg">立即注册</button>
        </form>

        <div class="auth-footer">
            已有账号？<a href="${pageContext.request.contextPath}/login.jsp">直接登录</a>
        </div>
    </div>
</div>

<script>
    const ctx = '${pageContext.request.contextPath}';

    // ===== Ajax 唯一性校验（任务书要求：切换焦点后即调用 Ajax 校验） =====
    function checkUnique(field, value, tipId) {
        if (!value) return;
        const xhr = new XMLHttpRequest();
        xhr.open('GET', ctx + '/checkUnique?field=' + field + '&value=' + encodeURIComponent(value), true);
        xhr.onload = function() {
            if (xhr.status === 200) {
                try {
                    const r = JSON.parse(xhr.responseText);
                    const tip = document.getElementById(tipId);
                    if (r.code === 200) {
                        tip.className = 'form-tip success';
                        tip.textContent = '✓ ' + r.msg;
                    } else {
                        tip.className = 'form-tip error';
                        tip.textContent = '✗ ' + r.msg;
                    }
                } catch (e) {}
            }
        };
        xhr.send();
    }

    document.getElementById('username').addEventListener('blur', function() {
        if (this.value.length >= 3) checkUnique('username', this.value, 'usernameTip');
    });
    document.getElementById('email').addEventListener('blur', function() {
        if (this.value) checkUnique('email', this.value, 'emailTip');
    });
    document.getElementById('phone').addEventListener('blur', function() {
        if (/^1[3-9]\d{9}$/.test(this.value)) checkUnique('phone', this.value, 'phoneTip');
    });

    // 密码确认
    document.getElementById('password2').addEventListener('blur', function() {
        const tip = document.getElementById('password2Tip');
        if (this.value !== document.getElementById('password').value) {
            tip.className = 'form-tip error';
            tip.textContent = '✗ 两次密码不一致';
        } else if (this.value) {
            tip.className = 'form-tip success';
            tip.textContent = '✓ 密码一致';
        }
    });

    // ===== 三级行政区联动（Ajax） =====
    function loadRegion(level, parent, selectEl, placeholder) {
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
                        selectEl.appendChild(opt);
                    });
                } catch (e) {}
            }
        };
        xhr.send();
    }

    const provinceEl = document.getElementById('province');
    const cityEl = document.getElementById('city');
    const districtEl = document.getElementById('district');

    // 初始加载省份
    loadRegion('province', null, provinceEl, '请选择省份');

    provinceEl.addEventListener('change', function() {
        cityEl.innerHTML = '<option value="">请选择城市</option>';
        districtEl.innerHTML = '<option value="">请选择区/县</option>';
        if (this.value) loadRegion('city', this.value, cityEl, '请选择城市');
    });
    cityEl.addEventListener('change', function() {
        districtEl.innerHTML = '<option value="">请选择区/县</option>';
        if (this.value) loadRegion('district', this.value, districtEl, '请选择区/县');
    });

    // 表单提交前最后校验
    document.getElementById('registerForm').addEventListener('submit', function(e) {
        if (document.getElementById('password').value !== document.getElementById('password2').value) {
            alert('两次输入的密码不一致');
            e.preventDefault();
        }
    });
</script>
</body>
</html>
