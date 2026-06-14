package com.opentorelocate.practice.dsaarray;

public class ArraysPractice {
//    sbse pehla questiojn ek demo ka hai,
    public static void main(String args[]){
        System.out.println("Testing correctly");
        int missingNUmberArray[] = {1,2,3,4,5,6,7};
        int missingNUmberArray2[] = {1,2,3,1,5};
        Integer missingNUmber = findMissingNUmber(missingNUmberArray2, missingNUmberArray);
//        change this to once with freq and other time for freq2
        System.out.println(missingNUmber);
    }

//    Question 1 is 1,2,4,5,6,1 -> print 3 :::: 1,2,3 -> print 4
//    and else case for - values return 1;

    public static Integer findMissingNUmber(int[] num2, int[] num){
//        maine socha hai ek frequency array lelete hai aur add krte rahenge jaha bhi vo number
//        present honge true krke freq[true at 0location, false at 1location, true at 3location]
        boolean freq[] = new boolean[num.length+1];
        boolean freq2[] = new boolean[num2.length+1];
        for(int i:num ){
            if(i>0 && i<=num.length) freq[i] = true;
        }
        for(int i:num2 ){
            if(i>0 && i<=num2.length) freq2[i] = true;
        }
        for(int i =1; i<=num.length; i++){
//            jo present nhi hai uske dedo
            if(!freq[i]) return i;
        }
//        for(int i =1; i<=num2.length; i++){
//            if(!freq2[i]) return i;
//        }
//        ab agar bhai sb present hai yaar
        return num.length+1;
//        return num2.length+1;
    }
}
