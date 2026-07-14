package com.opentorelocate.practice.dsastrings;

public class StringsPractice {
//    Print a string test
    public static void main(String[] args) {
        System.out.println("Hi everyone, new tutorial *:53AM 29/05.. OpenToRelocate " +
                "Open to relocate Outside India");
        stringManipulation("Reverse string + reverse words + reverse letters");
        System.out.println("Non repeating-->" + repeatingNonRepeatingPalindrome("abbcccdaes", "Non Repeating Characters"));
        System.out.println("repeatingCharacter-->" + repeatingNonRepeatingPalindrome("abbcccdaes", "Repeating Characters"));
        System.out.println("palindrome-->" + repeatingNonRepeatingPalindrome("abbcccdaes", "palindrome"));
    }
//    Reverse words, reverse string, reverse letters
    public static void stringManipulation(String string){
        StringBuffer reverseWords = new StringBuffer();
        char ch[] = new char[string.length()];
//        reverse words mtlb poora word ghuma do
//        I love java -> Java love I
        String[] reversWordsString = string.split(" ");
        for(int i=reversWordsString.length-1; i>=0; i--){
            reverseWords.append(reversWordsString[i]);
        }
        System.out.println(reverseWords);
//        clean the buffer orr builder
        reverseWords.setLength(0);

//        ab hogi string reverse
        char[] arr = string.toCharArray();
        for(int i=string.length()-1; i>=0; i--){
            reverseWords.append(arr[i]).trimToSize();
        }
        System.out.println(reverseWords);
        reverseWords.setLength(0);

//        reverse letters bhai
//        "I love Java" → "I evol avaJ"
//         character array lelo aur usko ulta krdo hr string word ko
//        reverse words mtlb poora word ghuma do
        String[] reversLetters = string.split(" ");
        for(int i=0; i<reversLetters.length; i++){
            char chReverseWords[] = reversLetters[i].toCharArray();
            for (int j = chReverseWords.length - 1; j >= 0; j--) {
                reverseWords.append(chReverseWords[j]);
            }
            reverseWords.append(" ");
        }
        System.out.println(reverseWords);
        reverseWords.setLength(0);
    }

//    Find non repeating character, find repeating character, valid palindrome(ignore special case)

      public static char repeatingNonRepeatingPalindrome(String str, String type){
        int freq[] = new int[256];
        switch(type) {
            case "Non Repeating Characters": {
                char ch[] = str.toCharArray();
                for (char chLetter : ch) {
                    if (chLetter != ' ') {
                        freq[chLetter]++;
                    }
                }
                for (char chLetter : ch) {
                    if (freq[chLetter] == 1) return chLetter;
                }
                return ' ';
            }
            case "Repeating Characters": {
                char ch[] = str.toCharArray();
                for (char chLetter : ch) {
                    if (chLetter != ' ') {
                        freq[chLetter]++;
                    }
                }
                for (char chLetter : ch) {
                    if (freq[chLetter] > 1) return chLetter;
                }
                return ' ';
            }
            case "palindrome": {
                char ch[] = str.toCharArray();
                for (char chLetter : ch) {
                    if (chLetter != ' ') {
                        freq[chLetter]++;
                    }
                }
                for (char chLetter : ch) {
                    if (freq[chLetter] > 1) return chLetter;
                }
                return ' ';
            }
            default:
                return ' ';
        }

      }
//    Longest common prefix, Anagram check(without sorting), group anagram

//    date -> 18/06/2026  5-7 questions of strings


//    date -> 19/06/2026  5-7 questions of strings


//    date -> 18/06/2026  5-7 questions of strings

//    date -> 19/06/2026  5-7 questions of strings

//    date -> 20/06/2026  5-7 questions of strings

//    date -> 21/06/2026  5-7 questions of strings

//    date -> 22/06/2026  5-7 questions of strings
}
