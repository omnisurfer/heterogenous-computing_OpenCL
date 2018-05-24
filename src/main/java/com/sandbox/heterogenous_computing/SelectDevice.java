/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sandbox.heterogenous_computing;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import java.util.Scanner;

/**
 *
 * @author danielrowan
 */
public class SelectDevice {
    public static void main(String args[]) throws Exception {
        
    }
    
    public static int SelectDevice(CLContext context, CLDevice devices[]) throws Exception {
        System.out.println("SelectDevice()");                                     

            System.out.println(context +
                "\n\tMax FLOPS device: " + context.getMaxFlopsDevice().getName()
            );
                                                  
            System.out.println("\nmax FLOPS device: " + context.getMaxFlopsDevice().getName());  

            Scanner in = new Scanner(System.in);
            boolean running = true;
            while(running)                        
            {
                listDevices(devices);

                System.out.print("Select compute device, e/E to exit: ");
                if( in.hasNextInt() )
                {
                    int inIndex = in.nextInt();

                    if(inIndex >= 0 && inIndex <= devices.length - 1)
                    {
                        System.out.println("Device selected");
                        return inIndex;                    
                    }

                    System.out.println();
                }
                else if( in.hasNextLine() )
                {
                    String line = in.nextLine();

                    if(line.contains("e") || line.contains("E")) {                    
                        return -1;
                    }
                }
                else
                    try {
                        Thread.sleep(100);
                    }
                    catch(InterruptedException ex) {
                        return -1;
                    }
            } 
            // </editor-fold>
            return -1;        
    }
    
    private static void listDevices(CLDevice devices[])
    {
        for(int i = 0; i < devices.length; ++i)          
        {   
            System.out.println(
                    "Device Index: " + i + " - " + devices[i]
            );
            System.out.println(
                String.format(
                    "\tmaxComputeUnits: %d"
                    + "\n\tmaxWorkgroup Size: %d"
                    + "\n\tmaxClockFreqency (MHz) %d"
                    + "\n\tglobalMemSize (MB): %d"
                    + "\n\tlocalMemSize (MB): %d",                        
                    devices[i].getMaxComputeUnits(),
                    devices[i].getMaxWorkGroupSize(),
                    devices[i].getMaxClockFrequency(),
                    devices[i].getGlobalMemSize()/(1024*1024),
                    devices[i].getLocalMemSize()/(1024*1024)                        
                )
            );
        }        
    }
}
