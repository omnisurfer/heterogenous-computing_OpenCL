/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sandbox.heterogenous_computing;

import java.util.Scanner;

/**
 * Possible help with compiling: https://imagej.net/A_Tutorial_for_using_OpenCL_in_ImageJ
 * https://stackoverflow.com/questions/16506297/how-to-use-dylib-file
 * https://www.chilkatsoft.com/java-loadLibrary-MacOSX.asp (ended up using: java -Djava.library.path="./lib" -jar dist/jocl-template.jar)
 * 
 * 
 * @author danielrowan
 */
public class Main {
    public static void main(String[] _args) throws Exception {
        
        Scanner in = new Scanner(System.in);
        boolean running = true;
        while(running)
        {
            listExamples();
            System.out.print("Enter your selection, or q/Q to quit: ");
            if( in.hasNextLine() )
            {
                String line = in.nextLine();
                running = selected(line, _args);
                System.out.println();
            }
            else
                try {
                    Thread.sleep(100);
                }
                catch(InterruptedException ex) {
                    return;
                }
        }        
    }
    
    private static boolean selected(String line, String[] args) throws Exception
    {
        if( line.toUpperCase().equals("Q") )
           return false;

        switch(line)
        {
            case "1":
                com.sandbox.heterogenous_computing.ch3_6_example.main(args);
                break;

            case "2":
                com.sandbox.heterogenous_computing.ch4_2_histogram.main(args);
                break;
                
            case "3":
                com.sandbox.heterogenous_computing.ch4_3_rotation.main(args);
                break;
                
            case "4":
                com.sandbox.heterogenous_computing.ch4_4_convolution.main(args);
            break;
                
            default:
                System.out.println("Invalid selection.");
        }
        return true;        
    }
    
    private static void listExamples()
    {
    System.out.println("Heterogenous Computing Examples");
        System.out.println(
            "Select which example to run:"
            + "\n\t1) Ch 3.6, Example"
            + "\n\t2) Ch 4.2, Histogram"
            + "\n\t3) Ch 4.3 Rotation"
            + "\n\t4) Ch 4.4 Convolution"
        );        
    }
}
