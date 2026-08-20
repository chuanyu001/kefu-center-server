-- 腾讯文档管理：为 platform=tencent 的在线文档填充占位正文（正文为空时）
-- 说明：这些腾讯文档(docs.qq.com)/金山文档(kdocs.cn)为在线文档，需登录访问，无法自动抓取正文，
--       故先写入占位内容，便于前端点击标题直接渲染正文；真实正文待用户粘贴/导出后导入。
USE kefu_center;

UPDATE documents
SET
  content = CONCAT(
    '<h3>', title, '</h3>',
    '<p><strong>正文待补充</strong>：该文档为腾讯文档/金山文档在线文档，需登录访问，暂无法自动抓取正文。</p>',
    '<p>请点击右下角「编辑文档」，将正文粘贴到编辑器中保存；或将文档导出为 Word/HTML/文本后交给系统导入。</p>',
    '<p>原始链接：<a href="', url, '" target="_blank" rel="noopener">', url, '</a></p>'
  ),
  format = 'html'
WHERE platform = 'tencent'
  AND (content IS NULL OR content = '');
