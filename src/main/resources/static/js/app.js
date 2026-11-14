/* ============================================================
   校园二手闲置交易平台 - 公共脚本
   说明：全部为原生 JS，不依赖任何 CDN，答辩现场断网也可正常运行
   ============================================================ */
(function () {
    'use strict';

    /**
     * 分页：点击页码时保留当前地址栏上的所有筛选参数（关键词、分类、价格区间、状态等）
     */
    function bindPager() {
        document.querySelectorAll('.pager a[data-page]').forEach(function (link) {
            link.addEventListener('click', function () {
                var page = link.getAttribute('data-page');
                var url = new URL(window.location.href);
                url.searchParams.set('pageNum', page);
                window.location.href = url.toString();
            });
        });
    }

    /**
     * 危险操作二次确认（不使用浏览器弹窗，避免打断体验）
     * 用法：给按钮加 data-confirm="确定要删除吗？"
     */
    function bindConfirm() {
        document.querySelectorAll('[data-confirm]').forEach(function (el) {
            var original = el.tagName === 'INPUT' ? el.value : el.textContent;
            var armed = false;
            var timer = null;

            function disarm() {
                armed = false;
                if (el.tagName === 'INPUT') {
                    el.value = original;
                } else {
                    el.textContent = original;
                }
            }

            el.addEventListener('click', function (e) {
                if (armed) {
                    return; // 第二次点击，放行
                }
                e.preventDefault();
                armed = true;
                var tip = el.getAttribute('data-confirm') || '再点一次确认';
                if (el.tagName === 'INPUT') {
                    el.value = tip;
                } else {
                    el.textContent = tip;
                }
                clearTimeout(timer);
                timer = setTimeout(disarm, 3000);
            }, true);
        });
    }

    /**
     * 详情页图集切换
     */
    function bindGallery() {
        var main = document.querySelector('.gallery-main img');
        if (!main) {
            return;
        }
        var thumbs = document.querySelectorAll('.gallery-thumbs [data-large]');
        thumbs.forEach(function (thumb, index) {
            if (index === 0) {
                thumb.classList.add('active');
            }
            thumb.addEventListener('click', function (e) {
                e.preventDefault();
                var src = thumb.getAttribute('data-large');
                if (src) {
                    main.src = src;
                }
                thumbs.forEach(function (t) {
                    t.classList.remove('active');
                });
                thumb.classList.add('active');
            });
        });
    }

    /**
     * 发布物品：本地预览已选择的图片，并限制最多 5 张
     */
    function bindUploadPreview() {
        var input = document.getElementById('imageInput');
        var box = document.getElementById('imagePreview');
        if (!input || !box) {
            return;
        }
        input.addEventListener('change', function () {
            box.innerHTML = '';
            var files = Array.prototype.slice.call(input.files || []).slice(0, 5);
            files.forEach(function (file) {
                if (!file.type || file.type.indexOf('image/') !== 0) {
                    return;
                }
                var img = document.createElement('img');
                img.className = 'preview-thumb';
                img.src = URL.createObjectURL(file);
                box.appendChild(img);
            });
        });
    }

    /**
     * 图片上传大小校验（后端也会校验，这里提前提示，减少无效请求）
     */
    function bindUploadLimit() {
        var input = document.getElementById('imageInput');
        if (!input) {
            return;
        }
        var maxBytes = 5 * 1024 * 1024;
        input.addEventListener('change', function () {
            var files = input.files || [];
            for (var i = 0; i < files.length; i++) {
                if (files[i].size > maxBytes) {
                    input.value = '';
                    var box = document.getElementById('imagePreview');
                    if (box) {
                        box.innerHTML = '<div class="form-error">单张图片不能超过 5MB，请重新选择</div>';
                    }
                    return;
                }
            }
        });
    }

    /**
     * 我的消息：点击整条消息即提交到 /user/messages/{id}/open
     * 后端会先把该消息标记为已读，再跳转到关联的订单 / 物品页面
     */
    function bindMessageOpen() {
        document.querySelectorAll('.msg-item.msg-clickable').forEach(function (item) {
            var form = item.querySelector('.msg-open-form');
            if (!form) {
                return;
            }
            item.addEventListener('click', function (e) {
                var tag = (e.target.tagName || '').toLowerCase();
                // 点在按钮、链接、输入框或它们内部时不触发跳转，避免和「标为已读」等按钮冲突
                if (tag === 'button' || tag === 'a' || tag === 'input' || e.target.closest('button, a, input, form[method="post"]')) {
                    return;
                }
                form.submit();
            });
        });

        // 「查看订单 / 查看物品」按钮同样走「先标记已读、再跳转」的流程
        document.querySelectorAll('[data-open-message]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var form = btn.closest('.msg-item') && btn.closest('.msg-item').querySelector('.msg-open-form');
                if (form) {
                    form.submit();
                }
            });
        });
    }

    /**
     * 自动隐藏成功提示（3 秒），失败提示保留
     */
    function autoHideFlash() {
        document.querySelectorAll('.flash-success').forEach(function (el) {
            setTimeout(function () {
                el.style.transition = 'opacity .4s';
                el.style.opacity = '0';
                setTimeout(function () {
                    el.remove();
                }, 400);
            }, 3000);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        bindPager();
        bindConfirm();
        bindGallery();
        bindUploadPreview();
        bindUploadLimit();
        bindMessageOpen();
        autoHideFlash();
    });
})();