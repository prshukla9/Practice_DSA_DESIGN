package com.opentorelocate.practice.streams;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
/*
* Collectors kya kr skte hai--> Collectors.collectingAndThen(Collectors.maxBy(Comparator.compa))
* Yaad rakho bhai
                toList()	List bana deta hai
                groupingBy()	Map bana deta hai
                maxBy()	max element deta hai
                averagingDouble()	average nikaalta hai

 */
public class StreamsPractice {

// Non repeating and repeating characters HAHHA
    /*.chars()--> ascii me change krdo string ko
    .mapToObj(c-> chars(c)) vaapis 98, 97--> ['b','a']
    ab group banao iss map ka aur maintain karo inka insertion order using linkedHashMap
    .collect(Collectors.groupingBy((Function.identity(), LinkedHashMap::new ,Collectors.counting()))
    map ban chuka hai unke numbers ke saath
    .entrySet.stream() map ko tsream me convert aur netry set kya krta hai [(k,v),(k,v),(k,v)] aise stream me read ho jaayeg a
    .filter(e->e.getValue()>1) vo values nikaal lo map se .map(Map.Entry::getKey)--> aur vo saare keys dedo entry = (k,value)
    take key only toh Map.Entry::getKey --> Map.Entry<key, value>
    .findFirst().orElse(null);
    .map()-->
    */

    public static void main(String args[])
    {
    String nonRepeating = "abbadacdee";
    Character nonRepeatingChar =
            nonRepeating.chars().mapToObj(c -> (char) c)
                    .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
                    .entrySet().stream().filter(value -> value.getValue() < 2).map(Map.Entry::getKey)
                    .findFirst().orElse(null);
    System.out.println(nonRepeatingChar);

    String repeating = "abcdesddrr";
    System.out.println(
            repeating.chars().mapToObj(c->(char)c)
                    .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
                    .entrySet()
//                    ab map me entrySet --> [(k,v),(k,v),(k,v),(k,v)] lelo bhai key value strema me
                    .stream().filter(values->values.getValue()>=2)
                    .map(Map.Entry::getKey)
//                    Map.Entry--> enytry = [K, V] toh keys nikaal lo
                    .findFirst().orElse(null)
    );

    List<EmployeeStreamClass> employee = Arrays.asList(
            new EmployeeStreamClass(1, "Aman", "HR", 400000),
            new EmployeeStreamClass(2, "Rahul", "IT", 700000),
            new EmployeeStreamClass(3, "Neha", "HR", 450000),
            new EmployeeStreamClass(4, "Sita", "Finance", 300000),
            new EmployeeStreamClass(5, "John", "HR", 600000)
    );
    List<EmployeeStreamClass> employeeSalaryFilter = findHrDeptAndMoreSalary(employee);
    employeeSalaryFilter.forEach(System.out::println);
//    -------------------------------level wise part 1-------------------------------
        part1(employee);
        part2(employee);
//        --------------------------------part 3-----------------------------------------
        part3(employee);
}


    public static List<EmployeeStreamClass> findHrDeptAndMoreSalary(List<EmployeeStreamClass> employeeStreamClasses){

        return (
                employeeStreamClasses.stream().filter(value->value.department.equals("HR"))
                        .filter(employeeSalary-> employeeSalary.salary > 500000)
//                        or you can write .filter(value->value.department.equals("HR") && value.salary>50000)
//                        .forEach(System.out::println)
                        .collect(Collectors.toList())
                );
    }
    public static List<EmployeeStreamClass> findHighestSalary(List<EmployeeStreamClass> employeeStreamClasses){

        return (
                employeeStreamClasses.stream().filter(value->value.department.equals("HR"))
                        .filter(employeeSalary-> employeeSalary.salary > 500000)
//                        or you can write .filter(value->value.department.equals("HR") && value.salary>50000)
//                        .forEach(System.out::println)
                        .collect(Collectors.toList())
                );
    }
    public static List<EmployeeStreamClass> findGroupByDepartment(List<EmployeeStreamClass> employeeStreamClasses){

        return (
                employeeStreamClasses.stream().filter(value->value.department.equals("HR"))
                        .filter(employeeSalary-> employeeSalary.salary > 500000)
//                        or you can write .filter(value->value.department.equals("HR") && value.salary>50000)
//                        .forEach(System.out::println)
                        .collect(Collectors.toList())
                );
    }
    public static List<EmployeeStreamClass> findAverageSalary(List<EmployeeStreamClass> employeeStreamClasses){

        return (
                employeeStreamClasses.stream().filter(value->value.department.equals("HR"))
                        .filter(employeeSalary-> employeeSalary.salary > 500000)
//                        or you can write .filter(value->value.department.equals("HR") && value.salary>50000)
//                        .forEach(System.out::println)
                        .collect(Collectors.toList())
                );
    }

    /* Part 1
    * List of employees me se HR department wale print karo
        Employees ki salary > 500000 filter karo
        All employee names list me collect karo
        Total salary find karo (sum)
        Average salary find karo
        Count number of employees
    * */

    public static void part1(List<EmployeeStreamClass> employeeStreamClasses){
//            hr department vaale print karo
        employeeStreamClasses.stream().filter(e->e.department.equals("HR"))
                .forEach(System.out::println);
//        salary 50000 filter karo
        employeeStreamClasses.stream().filter(e->e.salary>50000)
                .forEach(System.out::println);
//        All employee names list me collect karo
        List<String> names = employeeStreamClasses.stream().map(employee->employee.name).collect(Collectors.toList());
        System.out.println(names);
//        Total salary (sum)
//        total salary nikaalne ke liye aggregator aur bhai aggeregator map use hoga mapToINteger ya double
        System.out.println(
                employeeStreamClasses.stream().mapToDouble(e->e.salary).sum()
        );
//        Average salary
        System.out.println(
                employeeStreamClasses.stream().mapToDouble(e->e.salary).average()
                        .orElse(0.0)
        );
//        Count number of employees
//        bhai krni hai counting
        System.out.println("Count" +
                employeeStreamClasses.stream().count()
        );
    }


    /*
    * Top 3 highest salary employees
        Lowest salary employee find karo
        Employees ko salary ke basis pe sort karo (asc/desc)
        Employees ko department ke basis pe group karo
        Har department ka employee count nikaalo
        Partition karo (salary >= 500k vs < 500k)
        HR department ka average salary
* */

    public static void part2(List<EmployeeStreamClass> employeeStreamClasses){
//        Top 3 highest salary employees
//        toh bhai yaha pehle compare karo reverse krdo aur limit laga kr 3 nikaal lo
        System.out.println("top 3 highest salaries List<Employee> list = " +
                employeeStreamClasses.stream().sorted(Comparator.comparingDouble((EmployeeStreamClass e)->e.salary).reversed()).limit(3)
                        .collect(Collectors.toList())
        );

//        Lowest salary employee
//        Bhai lowest salary batao yaara
//        stream lagao, min lagao compare krdo comparingInteger
        System.out.println("Lowest salary of employee Employee lowest = " +
                employeeStreamClasses.stream().min(Comparator.comparingDouble(e->e.salary))
                        .orElse(null)
        );

//        Sort by salary (ASC / DESC)
//        ab bhai yaha vaapis sorted laga comparator comparingDouble and comparingDouble .reversed
//        ek hack hai bhai yaha kabhi kabhi ye smjh nahi paata e->e.salary comparing me toh
//        (Employee____ e) -> e.salary likh do chl jaayega

//        Group by department
//        Bhai ab department me grouping by krde and map me daal de
        System.out.println("Ye save hoga Map<String, List<Employee>> = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e->e.department))
        );

//        Employee count per department
//          Bhai bata de ab collectors groupingBy krke department me count laga de employees ke liye
//        groupingBy(Function.indentity, Collectors.counting())
//        -->ab kyunki hume function identity pata hai yaha that is department so e->e.department

        System.out.println("Ye save hoga Map<String, Long> = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e->e.department, Collectors.counting()))
        );

//        Partition (>=500k vs <500k)
        System.out.println("Ye save Map<Boolean, List<Employee>> partition =" +
                employeeStreamClasses.stream().collect(Collectors.partitioningBy(e->e.salary>=50000))
        );
//System.out.println(">=500k: " + partition.get(true));
//System.out.println("<500k: " + partition.get(false));
//
//        HR department ka average salary
        System.out.println("average salary double salary =" +
                employeeStreamClasses.stream().filter(e->e.department.equals("HR"))
                        .mapToDouble(e->e.salary).average().orElse(0.0)
                );
    }
    /*
    * First non-repeating character (string stream)
        First repeating character
        Unique elements list (distinct)
        Duplicate elements find karo
        Map<Character, count> create karo (frequency map)
        Most frequent character find karo
    * */

    /*
    *   Employees ke saare unique skills nikaalo (flatMap)
        HR employees ke unique skills
        Max salary per department
        Min salary per department
        Employee with second highest salary
        Longest string in list
        Group strings by length
    * */
//    HR employees me se top 2 salary + names only
//    bhai top 2 salaries aur sirf naam
//    toh limit lagade sorted krke and map laga kr sirf naam nikaal le aur list me daal de
    public static void part3(List<EmployeeStreamClass> employeeStreamClasses){

        System.out.println("Name from top salaries and department HR List<String> names = " +
            employeeStreamClasses.stream().filter(e->e.department.equals("HR"))
                    .sorted(Comparator.comparingDouble((EmployeeStreamClass e)->e.salary).reversed())
                    .limit(2).map(e->e.name).collect(Collectors.toList())
        );


//    HR employees ke unique skills
//    suppose skills are in List<String> flat map karo istemaal bhai

//        System.out.println(" HR employees List<String> skills = " +
//                employeeStreamClasses.stream().filter(e -> e.department.equals("HR"))
//                        .flatMap(e->e.skills.stream())
//                        .distinct().toList()
//        );

//        Max salary per department
//        ab bhai pehle kkaro group inko departments ko phir then comparing collectors maxBy
//        Bhai toh kya kara humne pata haina--> collectors use kiya collect krne ke lite group me
//        phir jb department liya toh Collectors use kiya maxBy comparator lagane ke liye
//
                System.out.println(" Max slaries of employee per department Map<String, Double> maxSlary = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e->e.department
                        ,Collectors.maxBy(Comparator.comparingDouble(e->e.salary))))

        );
//        Min salary per department
        System.out.println(" Min slaries of employee per department Map<String, Double> maxSlary = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e->e.department,
                        Collectors.minBy(Comparator.comparingDouble(e->e.salary))))
        );

//        Employee with second highest salary
//        second highest mtlb skip krdo bhai ek reverse krke and find()
        System.out.println(" Second Highest slaries of employee Double salary = " +
                employeeStreamClasses.stream().sorted(Comparator.comparingDouble((EmployeeStreamClass e)->e.salary)
                        .reversed()).skip(1).findFirst().orElse(null)
        );

//        Longest string in list
        List<String> listOfString = Arrays.asList("Hello", "Black", "Buffalo", "with", "Small", "nails");
        System.out.println(" String longestString =  = " +
                listOfString.stream().max(Comparator.comparingInt(elements->elements.length()))
//                ya iska function likh do (String::lenght)
        );
//        Group strings by length
//        listOfString
        System.out.println(" Group the Strings Map<Integer, List<String>> maxSlary = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e->e.department))
        );
    }


    /*
    * HR employees me se top 2 salary + names only

        Employees ka Map<Department, Avg Salary>
        Flatten nested list (flatMap)
        Convert List → Map (id → employee)
        Sort employees by salary desc then name asc
    * */

    public static void part4(List<EmployeeStreamClass> employeeStreamClasses){
//        HR employees me se top 2 salary + names only
        System.out.println(" Max 2 salaries names.... List<String> result = " +
                employeeStreamClasses.stream().sorted(Comparator.comparingDouble((EmployeeStreamClass e)->e.salary).reversed())
                        .limit(2).map(e->e.name).toList()
        );
//        Employees ka Map<Department, Avg Salary>
        System.out.println(" Employees ka Map<String, Dounle> employees ka map = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e -> e.department, Collectors.averagingDouble(e->e.salary)))
        );
//        Flatten nested list (flatMap)
//        System.out.println(" list of skills List<String> skills = " +
//                employeeStreamClasses.stream().flatMap(e->e.skills.stream())
//                        .collect(Collectors.toList());
//        );
//        Convert List → Map (id → employee)
//        ab bhai yaha map use hoga aur map me vo convert karega key value me--> COllectors.toMap()
        System.out.println(" Max slaries of employee per department Map<String, Double> maxSlary = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e -> e.department))
        );

//        Sort employees by salary desc then name asc
        System.out.println(" Max slaries of employee per department Map<String, Double> maxSlary = " +
                employeeStreamClasses.stream().collect(Collectors.groupingBy(e -> e.department))
        );
    }
}
