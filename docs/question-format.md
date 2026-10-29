# 题库 TXT 格式

文件编码为 UTF-8，每行一题，字段之间使用真正的制表符（Tab），顺序固定：

`分类<Tab>题干<Tab>选项A<Tab>选项B<Tab>选项C<Tab>选项D<Tab>答案<Tab>解析`

答案只能是 A、B、C、D。前七个字段不能为空。格式错误行会报告行号；合法行仍会导入。重复题以“分类、题干、四个选项”的 SHA-256 指纹判重。示例见 `app/src/main/assets/sample_questions.txt`，其中题目为项目自编的格式演示题。
