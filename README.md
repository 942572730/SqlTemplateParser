##һ���򵥵�sql�����滻���棬֧�ֶ�̬����������̬����

## ��Ҫ�����Զ������ߣ����棩�ϡ�����֧��sql���ò���

## �÷�


1. ������������ƴ��

> \#{pName#{pName}} �� ����pNameֵΪ5,���Ƚ�����#{pName5}��Ȼ����ȡpName5��ֵ�滻���������ڶ�̬������ select * from table_#{month} where id = ${id}.

2. �����滻

> ${parameter} �� �滻Ϊ ? ,ͬʱ���ö�Ӧλ�õĲ������󣬹�prepareStatementʹ��

> ${parameter#{pName}} : �������pNameֵΪ5,�������Ƚ�����${parameter5},Ȼ����ȡֵ��
�����һ������parameter5ֵΪ"hello world"�����滻Ϊ?,�Ҷ�Ӧλ�õĲ���ֵ����Ϊ"hello world"

> @{arraysParameter} : ��������滻��������ݲ�Ϊ�գ��ҳ��ȴ���0 �����滻Ϊ?,?,? .���� sql��in , not in ��䣬�磺 where id in ('Nouse',@{Ids}) .

> $[optParam: statment ] : ֧�ֿ�ѡ��䣬�������optParam��ֵ��Ϊ�գ������[]�ڵ�statment,�����Ƴ�����statment�����ڴ����ѡ������

```sql
select * from shops where 1=1 $[shopName: and  shop_name = ${shopName} ] and status = 1 
```

> @[optArrays: statment ] : ��$[]������ͬ��������Ϊ���飬���߼������͡�

```sql
select * from shops where 1=1 @[Ids: and  id in ('Nouse',@{Ids})  ] and status = 1
```