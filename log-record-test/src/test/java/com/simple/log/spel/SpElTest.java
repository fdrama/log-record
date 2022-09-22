package com.simple.log.spel;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fdrama
 * @date 2022年08月24日 16:48
 */
@Slf4j
public class SpElTest {

    /**
     * 基本字面表达式
     */
    @Test
    public void test1() {
        ExpressionParser parser = new SpelExpressionParser();

        //字符串由单引号分隔。要将单引号本身放在字符串中，使用两个单引号字符
        Expression expression = parser.parseExpression("'Hello spEL'");

        String strVal = expression.getValue(String.class);

        System.out.println("1. Literal expression value: " + strVal);

    }

    /**
     * 数组 列表 Map
     */
    @Test
    public void test2() {
        ExpressionParser parser = new SpelExpressionParser();

        String[] arr = {"a", "b", "c"};
        Expression expression = parser.parseExpression("[0]");

        String value = expression.getValue(arr, String.class);

        log.info("array  : {}", value);

        List<String> strList = Arrays.asList("jack", "tom", "jerry");
        Expression expression1 = parser.parseExpression("[0]");

        String value1 = expression1.getValue(strList, String.class); // jack

        log.info("list : {}", value1);

        Map<String, String> map = new HashMap<>();
        map.put("name", "john");
        map.put("age", "18");
        Expression expression2 = parser.parseExpression("[name]");
        String value2 = expression2.getValue(map, String.class);  // john
        log.info("map : {}", value2);

    }

    /**
     * 运算符- 关系运算符 逻辑运算符 三目运算符 数学运算符 赋值操作符
     */
    @Test
    public void test3() {
        ExpressionParser parser = new SpelExpressionParser();

        Expression expression = parser.parseExpression("16 + 5");
        Integer intVal = expression.getValue(Integer.class);
        System.out.println("3. Mathematical operator value: " + intVal);

        Expression expression1 = parser.parseExpression("2 == 2");

        Boolean boolValue = expression1.getValue(Boolean.class);
        System.out.println("4. Boolean operator value : " + boolValue);


        String testString =
                parser.parseExpression("'test' + ' ' + 'string'").getValue(String.class);  // 'test string'

        System.out.println("5. Str operator value : " + testString);


        String falseString =
                parser.parseExpression("false ? 'trueExp' : 'falseExp'").getValue(String.class);
        System.out.println("6. Ternary operator value : " + falseString); // falseExp


        Inventor tesla = new Inventor("Nikola Tesla", "Serbian");
        StandardEvaluationContext context = new StandardEvaluationContext(tesla);

        String name = parser.parseExpression("Name?:'Elvis Presley'").getValue(context, String.class);

        System.out.println(name); // Nikola Tesla

        tesla.setName(null);

        name = parser.parseExpression("Name?:'Elvis Presley'").getValue(context, String.class);

        System.out.println(name); // Elvis Presley

    }

    @Test
    public void test4() {
        // create an array of integers
        List<Integer> primes = new ArrayList<>(Arrays.asList(2, 3, 5, 7, 11, 13, 17));

        // create parser and set variable 'primes' as the array of integers
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("primes", primes);

        // all prime numbers > 10 from the list (using selection ?{...})
        // evaluates to [11, 13, 17]
        List<Integer> primesGreaterThanTen =
                (List<Integer>) parser.parseExpression("#primes.?[#this>10]").getValue(context);

        System.out.println(primesGreaterThanTen);
    }

    /**
     * 变量
     */
    @Test
    public void test5() throws NoSuchMethodException {

        ExpressionParser parser = new SpelExpressionParser();

        Inventor tesla = new Inventor("Nikola Tesla", "Serbian");

        StandardEvaluationContext context = new StandardEvaluationContext(tesla);
        context.setVariable("newName", "Mike Tesla");

        parser.parseExpression("name = #newName").getValue(context);

        parser.parseExpression("name").setValue(context, tesla, "Nikola");

        System.out.println(tesla.getName()); // "Mike Tesla"
    }

    /**
     * 自定义函数
     */
    @Test
    public void test6() throws NoSuchMethodException {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 将方法将注册到计算上下文中
        context.registerFunction("lowerCase",
                StringUtils.class.getDeclaredMethod("lowerCase",
                        String.class));

        String helloWorldLowerCase =
                parser.parseExpression("#lowerCase('HELLO')").getValue(context, String.class);

        System.out.println(helloWorldLowerCase);
    }


    /**
     * 变量
     */
    @Test
    public void test7() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        Inventor inventor = new Inventor("tom", "china");
        // 变量设置到上下文中
        context.setVariable("_ret", inventor);

        Inventor value = parser.parseExpression("#_ret").getValue(context, Inventor.class);

        System.out.println(value.toString());

    }

    /**
     * 表达式模板化
     */
    @Test
    public void test8() {

        String expressionStr = "%{#user.name?:'管理员'} 修改了默认收件地址 - %{#oldAddress} 到 %{#newAddress} ";

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        Inventor inventor = new Inventor("tom", "china");
        // 变量设置到上下文中
        context.setVariable("user", inventor);
        context.setVariable("oldAddress", "重庆");
        context.setVariable("newAddress", "成都");

        ParserContext parserContext = new TemplateParserContext("%{", "}");
        // 表达式是一个模板表达式，需要为解析传入模板解析器上下文。
        String value = parser.parseExpression(expressionStr, parserContext).getValue(context, String.class);

        System.out.println(value);

    }

    public static class Inventor {

        private String name;
        private String nationality;
        private String[] inventions;
        private Date birthdate;
        private PlaceOfBirth placeOfBirth;


        public Inventor(String name, String nationality) {
            GregorianCalendar c = new GregorianCalendar();
            this.name = name;
            this.nationality = nationality;
            this.birthdate = c.getTime();
        }

        public Inventor(String name, Date birthdate, String nationality) {
            this.name = name;
            this.nationality = nationality;
            this.birthdate = birthdate;
        }

        public Inventor() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }

        public Date getBirthdate() {
            return birthdate;
        }

        public void setBirthdate(Date birthdate) {
            this.birthdate = birthdate;
        }

        public PlaceOfBirth getPlaceOfBirth() {
            return placeOfBirth;
        }

        public void setPlaceOfBirth(PlaceOfBirth placeOfBirth) {
            this.placeOfBirth = placeOfBirth;
        }

        public void setInventions(String[] inventions) {
            this.inventions = inventions;
        }

        public String[] getInventions() {
            return inventions;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Inventor.class.getSimpleName() + "[", "]")
                    .add("name='" + name + "'")
                    .add("nationality='" + nationality + "'")
                    .add("inventions=" + Arrays.toString(inventions))
                    .add("birthdate=" + birthdate)
                    .add("placeOfBirth=" + placeOfBirth)
                    .toString();
        }
    }

    public static class PlaceOfBirth {

        private String city;
        private String country;

        public PlaceOfBirth(String city) {
            this.city = city;
        }

        public PlaceOfBirth(String city, String country) {
            this(city);
            this.country = country;
        }


        public String getCity() {
            return city;
        }

        public void setCity(String s) {
            this.city = s;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    public static class Society {

        private String name;

        public static String Advisors = "advisors";
        public static String President = "president";

        private List<Inventor> members = new ArrayList<Inventor>();
        private Map officers = new HashMap();

        public List getMembers() {
            return members;
        }

        public Map getOfficers() {
            return officers;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isMember(String name) {
            boolean found = false;
            for (Inventor inventor : members) {
                if (inventor.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            return found;
        }


    }
}
