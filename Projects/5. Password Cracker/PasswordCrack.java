/*  This program cracks passwords with a dictionary brute force attack. Each word in the dictionary
    is hashed and compared to all encrypted entries in a given password file. After the initial search thru 
    each word is mangled with a number of algorithms. The mangled wordlist is then once again searched for matches
    and recursively mangles itself. The program has support for multithreading. The dictionary file is splitted up
    equally amongst the number of threads and each thread searches and mangles its own part of the dictionary. 
    
    Usage under UNIX: 
        javac PasswordCrack.java
        java PasswordCrack <dictionary> <passwd>
    
    @author Emil Stahl
    Date: May 09, 2020
*/

import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class PasswordCrack {

    public static ArrayList<String> nameList;
    public static CopyOnWriteArrayList<String> userPasswords;

    public static ArrayList<String> getDict(String dictionary) {

        ArrayList<String> temp = new ArrayList<String>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(dictionary))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                temp.add(line);
            }
        } catch (Exception e) {
            System.out.println("\nAn error occured while reading from file " + dictionary);
            System.exit(1);
        }
        return temp;
    }

    public static void getPasswords(String passwords) {

        userPasswords = new CopyOnWriteArrayList<String>();
        nameList = new ArrayList();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(passwords))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String splitted[] = line.split(":");
                String encryptedPassword = splitted[1];
                String[] username = splitted[4].split(" ");

                userPasswords.add(encryptedPassword);

                nameList.add(username[0]);
            }
        } catch (Exception e) {
            System.out.println("\nAn error occured while reading from file " + passwords);
            System.exit(1);
        }
    }

    public String checkPassword(String word, int id) {

        Iterator<String> iterator = userPasswords.iterator();

        while (iterator.hasNext()) {

            String password = iterator.next();
            String hash = jcrypt.crypt(password, word);

            if (userPasswords.contains(hash)) {
                System.out.println(word);
                userPasswords.remove(hash);
            }
        }
        return word;
    }

    public void mangle(ArrayList<String> dictList, int id, Algorithms algorithms) {

        ArrayList<String> mangleList = new ArrayList<String>();

        for (int i = 0; i < dictList.size(); i++) {

            String word = dictList.get(i).toString();

            if (word.length() != 0) {

                mangleList.add(checkPassword(algorithms.toLower(word), id));
                mangleList.add(checkPassword(algorithms.toUpper(word), id));
                mangleList.add(checkPassword(algorithms.capitalize(word), id));
                mangleList.add(checkPassword(algorithms.ncapitalize(word), id));
                mangleList.add(checkPassword(algorithms.reverse(word), id));
                mangleList.add(checkPassword(algorithms.mirror1(word), id));
                mangleList.add(checkPassword(algorithms.mirror2(word), id));
                mangleList.add(checkPassword(algorithms.toggle(word), id));
                mangleList.add(checkPassword(algorithms.toggle2(word), id));

                // If the word is bigger than eight, a duplicate word or a added letter won't change the hash.
                if (word.length() <= 8) {
                    mangleList.add(checkPassword(algorithms.deleteLast(word), id));
                    mangleList.add(checkPassword(algorithms.deleteFirst(word), id));
                    mangleList.add(checkPassword(algorithms.duplicate(word), id));

                    for (int j = 0; j <= 9; j++) {
                        checkPassword(algorithms.addNumberFirst(word, j), id);
                        checkPassword(algorithms.addNumberLast(word, j), id);
                    }

                    for (int k = 0; k < 26; k++) {
                        checkPassword(algorithms.addLetterLast(word, k), id);
                        checkPassword(algorithms.addLetterFirst(word, k), id);
                        checkPassword(algorithms.addLetterLastCap(word, k), id);
                        checkPassword(algorithms.addLetterFirstCap(word, k), id);
                    }
                }
            }
        }
        mangle(mangleList, id, algorithms);
    }

    public static ArrayList<String> addCommons(ArrayList<String> temp) {

        // Add common passwords
        temp.add("1234");
        temp.add("12345");
        temp.add("123456");
        temp.add("1234567");
        temp.add("12345678");
        temp.add("123456789");
        temp.add("1234567890");
        temp.add("qwerty");
        temp.add("abc123");
        temp.add("111111");
        temp.add("1qaz2wsx");
        temp.add("letmein");
        temp.add("qwertyuiop");
        temp.add("starwars");
        temp.add("login");
        temp.add("passw0rd");

        return temp;
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: <dictionary> <passwords>");
            System.exit(1);
        }

        PasswordCrack pCrack = new PasswordCrack();
        Algorithms algorithms = new Algorithms();
        ArrayList<String> dictList = new ArrayList<String>();

        String dictionary = args[0];
        String passwords = args[1];
        
        dictList = getDict(dictionary);
        getPasswords(passwords);

        dictList.addAll(nameList);
        dictList = addCommons(dictList);
   
        int threads = Runtime.getRuntime().availableProcessors();

        for (int id = 0; id < threads; id++) {
            final Worker worker = new Worker(id, threads, dictList, pCrack, algorithms);
            worker.start();
        }
    }
}

class Worker extends Thread {

    int id;
    int threads;
    ArrayList<String> dictList;
    PasswordCrack pCrack;
    Algorithms algorithms;

    public Worker(int id, int threads, ArrayList<String> dictList, PasswordCrack pCrack, Algorithms algorithms) {
        this.id = id;
        this.threads = threads;
        this.dictList = dictList;
        this.pCrack = pCrack;
        this.algorithms = algorithms;
    }

    public void run() {

        ArrayList<String> splitted = new ArrayList<String>();

        for (int i = id; i < dictList.size(); i += threads) {
            pCrack.checkPassword(dictList.get(i).toString(), id);
            splitted.add(dictList.get(i).toString());
        }
        pCrack.mangle(splitted, id, algorithms);
    }
}

class Algorithms {

    public static char[] letters = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    public void algorithms() {

    }

    public String addLetterLast(String word, int i) {
        char c = letters[i];
        return word + c;
    }

    public String addLetterFirst(String word, int i) {
        char c = letters[i];
        return c + word;
    }

    public String addLetterLastCap(String word, int i) {
        String c = String.valueOf(letters[i]);
        return word + c.toUpperCase();
    }

    public String addLetterFirstCap(String word, int i) {
        String c = String.valueOf(letters[i]);
        return c.toUpperCase() + word;
    }

    public String addNumberFirst(String word, int i) {
        return String.valueOf(i) + word;
    }

    public String addNumberLast(String word, int i) {
        return word + String.valueOf(i);
    }

    public String toUpper(String word) {
        return word.toUpperCase();
    }

    public String toLower(String word) {
        return word.toLowerCase();
    }

    public String deleteLast(String word) {
        return word.substring(0, word.length() - 1);
    }

    public String deleteFirst(String word) {
        return word.substring(1);
    }

    public String reverse(String word) {
        return new StringBuilder(word).reverse().toString();
    }

    public String duplicate(String word) {
        return word + word;
    }

    public String mirror1(String word) {
        return reverse(word) + word;
    }

    public String mirror2(String word) {
        return word + reverse(word);
    }

    public String capitalize(String word) {
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    public String ncapitalize(String word) {
        return word.substring(0, 1).toLowerCase() + word.substring(1).toUpperCase();
    }

    public String toggle(String word) {
        String toggled = "";

        for (int i = 0; i < word.length(); i++) {
            if (i % 2 == 0) {
                toggled += word.substring(i, i + 1).toUpperCase();
            } else {
                toggled += word.substring(i, i + 1);
            }
        }
        return toggled;
    }

    public String toggle2(String word) {
        String toggled = "";
        for (int i = 0; i < word.length(); i++) {
            if (i % 2 != 0) {
                toggled += word.substring(i, i + 1).toUpperCase();
            } else {
                toggled += word.substring(i, i + 1);
            }
        }
        return toggled;
    }
}