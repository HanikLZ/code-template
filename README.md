#### 模版语法说明
##### 模版指在模版文本中，动态替换掉指定内容，并生成指定文件的基本定义文本。模版除了内嵌的语法，其他部分等同于文本文件
1. 表达式
###### 形如`#{}`花括号中间的部分称为表达式，表达式在求值后，整体替换`#{}`部分
```
private #{type} #{name};
```
- 以上部分在实际生成文件时，可能会如下
```
private int value;
```
- 定义在`#{}`中表达式的可以基本的运算，如 `^` `*` `/` `+` `-`  `&&` `||`
###### 其优先级为排列顺序，不需要考虑数据类型，引擎会自动处理
```
private int age#{age + n};
```
- 表达式中可以使用函数，预定义函数目前仅有 `toUpperCase()` `toLowerCase()` `firstUpperCase()` `firstLowerCase()`
```
private int #{firstLowerCase(age)}
```
- 表达式中的字符串，不会求值，直接应用到结果中
```
private int #{"age" + age}   // age 会求值，"age"直接作为字符串处理
```
2. 语句块
###### 形如`!{}`花括号中间的部分定义语句块，语句块以`!{end}`结束，在生成文本时，整体替换语句块中间的部分
###### `!{}`中间也可以使用表达式，遵从上述`#{}`同定义
- `!{file 文件名}`文件块，其中定义的部分自动写入到指定的文件名中
```
!{file "target.java"}
class A {
}
!{end}
```
###### 以上将自动写入到当前目录下的`target.java`文件中
- `!{repeat var= collection=}`循环语句块，迭代指定集合，循环生成代码块中的内容。
```
!{repeat var=p collection=definitions}
private #{p} = 1;
!{end}

// 生成内容
private a = 1;
private b = 1;
...
```
- `!{if x=x}` `!{elif x=x}` `!{else}` 条件语句块，根据条件生成对应的语句块
```
!{if a="name"}
private int a = 2;
!{elif a="value"}
private int a = 3;
!{else}
private int a = 4;
!{end}
```
###### 代码块中可以只存在`!{if}`，或`!{if}` `!{else}`

4. 函数处理器
###### 可以使用自定义处理器来实现对函数的调用解析
```
public class MyFunctionProcessor implements FunctionProcessor {

    @Override
    public String processFunction(String s, List<String> list) {
    }
    
    
}

```
###### s 为函数名，如`toLowerCase`，list 中包含了参数列表，如`toLowerCase("MY")`中，list集合即包含了 my
###### 在实际模版中，表示为
```
#{toLowerCase(a)}
```
###### 若 a 并非字符串值，则会传入变量解析器中解析后，再放入list集合
###### 内置的函数包括
```
toLowerCase(a)
toUpperCase(a)
firstLowerCase(a)
firstUpperCase(a)
```

5. 变量处理器
###### 可以使用自定义处理器来实现对变量的解析
```
public class MyVariableProcessor implements VariableProcessor {

    @Override
    public String processVariable(String s) {
    }

    @Override
    public Collection<String> processKeyCollection(String s) {
    }
     
}
```
###### processVariable方法实现对变量的解析，例如`#{var}`会将 var 作为参数传入，输出字符串即可
###### processKeyCollection方法实现将变量解析成集合的工作，如`!{repeat var=a collection=b}`中的b变量

